#!/usr/bin/env python3
"""소스 스냅샷을 검증하고 전체 호출 SVG와 Obsidian 탐색 노트를 만든다."""
from collections import defaultdict
from hashlib import sha256
from html import escape
from pathlib import Path
from urllib.parse import urlencode
import json
import math
import re
import subprocess
import sys
import textwrap
import call_vault_guide as guide

BASE = Path(__file__).resolve().parents[1]
ROOT = next(p for p in BASE.parents if (p / 'src/main/java').is_dir())
VAULT = BASE / 'DodamDodam-Code-Map'
DOCS = ROOT / 'docs'
PREFIX = VAULT.relative_to(DOCS).as_posix() + '/'
DATA = json.loads((BASE / 'method-map.json').read_text())
CLASSES = {c['id']: c for c in DATA['classes']}
METHODS = {m['id']: m for m in DATA['methods']}
FEATURES = ['app', 'global', 'auth', 'child', 'guardian', 'conversation']
COLORS = {'app':'6', 'global':'5', 'auth':'2', 'child':'4', 'guardian':'3', 'conversation':'6'}


def uid(value, prefix='n'):
    """호출 데이터의 원래 식별자를 파일·Canvas에서 안전한 고정 ID로 바꾼다."""
    return prefix + sha256(value.encode()).hexdigest()[:16]


def feature(owner):
    """업무 기능을 기준으로 전체 지도와 기능별 지도의 범위를 구분한다."""
    name = owner.removeprefix('com.dodamdodam.backend.').split('.')[0]
    return name if name in FEATURES else 'app'


def class_path(owner):
    """동일한 단순 클래스 이름도 충돌하지 않도록 패키지를 파일명에 보존한다."""
    name = owner.removeprefix('com.dodamdodam.backend.')
    return 'Classes/' + re.sub(r'[^\w.\-]', '_', name) + '.md'


def heading(mid):
    """오버로드를 서로 다른 메서드 절로 연결한다."""
    m = METHODS[mid]
    return ('생성자' if m['name']=='<init>' else m['name']) + ' ' + uid(mid, 'm')


def short_type(text):
    """그림에서는 패키지 접두어를 줄이고 노트에는 원래 정의를 남긴다."""
    text = re.sub(r'@[\w.]+(?:\([^)]*\))?\s*', '', text)
    return re.sub(r'\b(?:[a-z_]\w*\.)+', '', text)


def signature(mid):
    """그림의 메서드 이름·인자·반환 타입과 자동 생성 여부를 표시한다."""
    m = METHODS[mid]
    sig = short_type(definition(mid))
    return sig + (' [자동]' if m['generated'] else '')


def definition(mid):
    """생성자의 컴파일러 내부 이름을 읽을 수 있는 Java 표기로 바꾼다."""
    m = METHODS[mid]
    return m['signature'].replace('void <init>', CLASSES[m['owner']]['name'])


def wiki(mid, label=None):
    """다른 클래스의 정확한 메서드 절을 가리키는 내부 링크를 만든다."""
    m = METHODS[mid]
    label = label or CLASSES[m['owner']]['name'] + '.' + ('생성자' if m['name']=='<init>' else m['name'])
    return '[[' + class_path(m['owner'])[:-3] + '#' + heading(mid) + '|' + label + ']]'


def obsidian_uri(owner, mid=None):
    """SVG에서 등록된 vault의 클래스 또는 메서드를 열 수 있게 한다."""
    file = str(VAULT / class_path(owner)) + ('#' + heading(mid) if mid else '')
    return 'obsidian://open?' + urlencode({'path':file,'paneType':'tab'})


def vault_links(text):
    """코드 사본을 보존하면서 문서 링크만 기존 docs 보관소 기준으로 바꾼다."""
    parts = re.split(r'(```[\s\S]*?```)', text)
    for index in range(0,len(parts),2):
        parts[index] = re.sub(r'\[\[([^\]|]+)',lambda m:'[['+(m[1] if m[1].startswith(('#',PREFIX)) else PREFIX+m[1]),parts[index])
    return ''.join(parts)


def save(relative, content):
    """작업 전용 vault 안에만 생성 결과를 기록한다."""
    path = VAULT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(content, dict):
        for node in content.get('nodes',[]):
            if node['type']=='file' and not node['file'].startswith(PREFIX):node['file']=PREFIX+node['file']
            if node['type']=='text':node['text']=vault_links(node['text'])
        content = json.dumps(content, ensure_ascii=False, indent=2)
    elif path.suffix=='.md':
        # Obsidian 표의 별칭 구분자가 열 구분자로 읽히지 않게 이스케이프한다.
        content='\n'.join(re.sub(r'\[\[([^\]]+)\]\]',lambda m:'[['+m[1].replace('\\|','|').replace('|','\\|')+']]',line)
                          if line.startswith('|') else line for line in content.splitlines())
        content = vault_links(content)
    path.write_text(content.rstrip() + '\n')


def verify_sources():
    """지도 생성 전 원본이 분석 시점과 같은지 확인해 오래된 호출선을 막는다."""
    for item in DATA['snapshot']['files']:
        path = ROOT / item['path']
        assert path.exists() and sha256(path.read_bytes()).hexdigest()==item['sha256'], item['path']


# 메서드 참조·생성·직접 호출은 같은 쌍의 관계 정보에 함께 보존한다.
PAIRS = defaultdict(list)
EXTERNAL = defaultdict(set)
for call in DATA['calls']:
    if call['to'] in METHODS:
        PAIRS[(call['from'],call['to'])].append(call)
    else:
        EXTERNAL[call['from']].add(call['to'])
INBOUND, OUTBOUND = defaultdict(list), defaultdict(list)
for (source,target), sites in PAIRS.items():
    OUTBOUND[source].append((target,sites))
    INBOUND[target].append((source,sites))
FRAMEWORK = defaultdict(list)
for item in DATA['framework']:
    FRAMEWORK[item['to']].append(item['label'])


def site_label(sites):
    """정적 참조를 즉시 호출로 오해하지 않도록 종류와 소스 줄을 함께 남긴다."""
    kinds = {'call':'호출','new':'생성','reference':'참조'}
    labels = '/'.join(kinds[k] for k in sorted({s['kind'] for s in sites}))
    return labels + ' · L' + ','.join(str(n) for n in sorted({s['line'] for s in sites}))


def write_class_notes():
    """클래스마다 모든 메서드 정의·코드·양방향 호출 링크를 기록한다."""
    for owner,c in CLASSES.items():
        methods = [m for m in METHODS.values() if m['owner']==owner]
        # 핵심 구현을 먼저 읽고 생성자·자동 멤버는 필요할 때 내려가서 확인한다.
        methods.sort(key=lambda m:(m['generated'],m['name']=='<init>',m['line']))
        primary = [m for m in methods if not m['generated'] and m['name']!='<init>']
        secondary = [m for m in methods if m not in primary]
        lines = ['---', 'tags: [backend, code-map, '+feature(owner)+']', 'aliases: ['+json.dumps(c['name'],ensure_ascii=False)+']', '---',
                 '# '+c['name'], '', '`'+owner+'`', '', '[[00-Start|시작]] → [[Features/'+feature(owner)+'|기능 개요]] → 이 클래스 · [[90-Reference|전체 참조 자료]]', '',
                 '## 클래스 요약', '', guide.class_summary(c,methods), '',
                 '- 원본: `'+c['file']+':'+str(c['line'])+'`',
                 '- 작성 메서드/생성자 '+str(sum(not m['generated'] for m in methods))+'개 · 자동 생성 '+str(sum(m['generated'] for m in methods))+'개', '', '## 메서드 목록', '']
        if primary:
            lines += ['| 메서드 | 하는 일 |','| --- | --- |']
            for m in primary:
                description=(m.get('doc') or '아래 구현과 호출 관계에서 확인').strip().split('\n')[0].replace('|',' / ')
                lines.append('| '+wiki(m['id'],m['name'])+' | '+description+' |')
        else:
            lines += ['데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.']
        if secondary:
            lines += ['','> [!example]- 생성자·자동 생성 멤버 '+str(len(secondary))+'개','>']
        for m in secondary:
            lines.append('> - '+wiki(m['id'],signature(m['id'])))
        for m in methods:
            mid = m['id']
            lines += ['', '## '+heading(mid), '', (m['doc'] or ('컴파일러가 생성한 멤버다.' if m['generated'] else '아래 원본 코드에서 구현을 확인한다.')).strip(), '', '```java',definition(mid),'```','']
            if m['annotations']: lines += ['설정: '+' / '.join('`'+a+'`' for a in m['annotations']), '']
            lines += ['### 이 메서드를 호출하는 곳','']
            if not INBOUND[mid] and not FRAMEWORK[mid]:lines += ['명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.','']
            for source,sites in sorted(INBOUND[mid]):
                if source in METHODS: label=wiki(source)
                else: label='[['+class_path(source.split('#')[0])[:-3]+'|필드/정적 초기화]]'
                lines.append('- '+label+' — '+site_label(sites))
            for label in FRAMEWORK[mid]: lines.append('- 프레임워크 진입: '+label)
            lines += ['', '### 이 메서드가 호출하는 곳', '']
            lines += ['- '+wiki(target)+' — '+site_label(sites) for target,sites in sorted(OUTBOUND[mid])] or ['프로젝트 내부 호출 없음.']
            if EXTERNAL[mid]:
                lines += ['', '### 외부 라이브러리 호출', '']
                lines += ['- `'+x+'`' for x in sorted(EXTERNAL[mid])]
            if not m['generated']:
                source = (ROOT/m['file']).read_text().splitlines()
                assert 1 <= m['line'] <= m['endLine'] <= len(source),mid
                body = '\n'.join(source[m['line']-1:m['endLine']])
                lines += ['', '### 소스 코드', '', '`'+m['file']+':'+str(m['line'])+'`', '', '```java',textwrap.dedent(body),'```']
            lines += ['', '[[Features/'+feature(owner)+'|기능 개요로 돌아가기]] · [[00-Start|다른 흐름 선택]]']
        save(class_path(owner),'\n'.join(lines))


def svg_graph(owners, output):
    """클래스를 메서드 행으로 나누고 실제 호출 쌍을 정확한 행에 연결한다."""
    selected={mid for mid,m in METHODS.items() if m['owner'] in owners}
    pairs={pair:sites for pair,sites in PAIRS.items() if pair[0].split('#')[0] in owners and pair[1] in selected}
    lines=['digraph Calls {', 'graph [rankdir=LR,bgcolor="white",pad=.35,nodesep=.3,ranksep=1.2,splines=polyline,fontname="Arial",label="전체 클래스 · 메서드 호출 지도 / 106 types · 566 members · 541 internal pairs\n실선: 호출·생성 / 점선: 메서드 참조 / 파선: 프레임워크 진입 / 회색 [자동]: 컴파일러 생성",labelloc=t];', 'node [shape=plain,fontname="Arial"];', 'edge [color="#84909a",penwidth=.65,arrowsize=.5,fontname="Arial",fontsize=9];']
    for f in FEATURES:
        group=sorted(o for o in owners if feature(o)==f)
        if not group:continue
        lines += ['subgraph cluster_'+f+' { label='+json.dumps(f)+'; color="#bac5cd";']
        for owner in group:
            c=CLASSES[owner]
            rows=['<TR><TD BGCOLOR="#e4edf4" HREF="'+escape(obsidian_uri(owner),quote=True)+'"><B>'+escape(c['name'])+'</B><BR/><FONT POINT-SIZE="9">'+escape(owner.removeprefix('com.dodamdodam.backend.'))+'</FONT></TD></TR>']
            for mid,m in METHODS.items():
                if m['owner']!=owner:continue
                wrapped='<BR ALIGN="LEFT"/>'.join(escape(line) for line in textwrap.wrap(signature(mid),105,break_long_words=False,break_on_hyphens=False))
                rows.append('<TR><TD PORT="'+uid(mid)+'" ALIGN="LEFT" HREF="'+escape(obsidian_uri(owner,mid),quote=True)+'" TOOLTIP="'+escape(m['signature'],quote=True)+'"><FONT POINT-SIZE="10" COLOR="'+('#6c737b' if m['generated'] else '#17212b')+'">'+wrapped+'</FONT></TD></TR>')
            if any(src==owner+'#<initializer>' for src,_ in pairs):
                rows.append('<TR><TD PORT="init" ALIGN="LEFT"><I>필드 / 정적 초기화</I></TD></TR>')
            lines.append(uid(owner)+' [id="'+uid(owner)+'",label=<<TABLE BORDER="1" CELLBORDER="1" CELLSPACING="0" CELLPADDING="5" COLOR="#a5b1bb">'+''.join(rows)+'</TABLE>>];')
        lines.append('}')
    for (source,target),sites in pairs.items():
        owner=source.split('#')[0]
        port=uid(source) if source in METHODS else 'init'
        target_owner=METHODS[target]['owner']
        ref=all(s['kind']=='reference' for s in sites)
        eid=uid(source+'>'+target,'edge')
        lines.append(uid(owner)+':'+port+':e -> '+uid(target_owner)+':'+uid(target)+':w [id="'+eid+'",style='+('dotted' if ref else 'solid')+',tooltip='+json.dumps(site_label(sites),ensure_ascii=False)+'];')
    for mid,labels in FRAMEWORK.items():
        if mid not in selected:continue
        for label in labels:
            rid=uid(label,'runtime')
            lines.append(rid+' [shape=box,style="rounded,dashed",fontsize=10,label='+json.dumps(label,ensure_ascii=False)+'];')
            lines.append(rid+' -> '+uid(METHODS[mid]['owner'])+':'+uid(mid)+':w [style=dashed,color="#6e7994"];')
    lines.append('}')
    dot=VAULT/'Assets'/output.with_suffix('.dot')
    dot.parent.mkdir(parents=True,exist_ok=True)
    dot.write_text('\n'.join(lines)+'\n')
    subprocess.run(['dot','-Tsvg',str(dot),'-o',str(dot.with_suffix('.svg'))],check=True)


def card_text(mid):
    """Canvas 카드에서 정확한 메서드 노트로 이동하는 링크를 제공한다."""
    m=METHODS[mid]
    return '### '+CLASSES[m['owner']]['name']+'.'+('생성자' if m['name']=='<init>' else m['name'])+'\n\n'+wiki(mid,'정의 · 소스 · 호출자 · 호출 대상')+'\n\n`'+short_type(definition(mid))+'`'+('\n\n자동 생성' if m['generated'] else '')


def method_canvas(focus=None):
    """전체 메서드를 보존하고 기능별 보기에는 직접 연결된 경계 메서드도 포함한다."""
    selected={mid for mid,m in METHODS.items() if focus is None or feature(m['owner'])==focus}
    primary=set(selected)
    pairs={pair:sites for pair,sites in PAIRS.items() if pair[0] in selected or pair[1] in selected}
    for source,target in pairs:
        if source in METHODS:selected.add(source)
        selected.add(target)
    nodes,edges=[],[]
    # 기능별 띠 안에서 클래스별 그룹을 배치해 좌표 겹침을 방지한다.
    ybase=0
    for f in FEATURES:
        owners=[o for o in CLASSES if feature(o)==f and any(METHODS[m]['owner']==o for m in selected)]
        if not owners:continue
        columns=[0,0,0]
        group_start=ybase
        for owner in owners:
            mids=[mid for mid,m in METHODS.items() if m['owner']==owner and mid in selected]
            column=min(range(3),key=columns.__getitem__)
            x=column*1640;y=ybase+columns[column]
            height=90+math.ceil(len(mids)/3)*260
            groupid=uid(owner,'group')
            nodes.append({'id':groupid,'type':'group','x':x,'y':y,'width':1560,'height':height,'label':owner.removeprefix('com.dodamdodam.backend.')+(' [경계]' if focus and not any(m in primary for m in mids) else ''),'color':COLORS[f]})
            for index,mid in enumerate(mids):
                nodes.append({'id':uid(mid),'type':'text','x':x+30+(index%3)*500,'y':y+60+(index//3)*260,'width':480,'height':235,'text':card_text(mid)})
            columns[column]+=height+120
        ybase+=max(columns)+230
        nodes.insert(0,{'id':uid(f+'band','group'),'type':'group','x':-55,'y':group_start-90,'width':4960,'height':max(columns)+70,'label':f+' · '+('업무 기능' if focus is None or f==focus else '직접 연결된 경계'),'color':COLORS[f]})
    # 초기화와 프레임워크 진입은 메서드로 가장하지 않고 별도 카드로 표시한다.
    special={source for source,_ in pairs if source not in METHODS}
    runtime={label for mid in selected for label in FRAMEWORK[mid]}
    for index,source in enumerate(sorted(special)):
        owner=source.split('#')[0]
        nodes.append({'id':uid(source),'type':'text','x':5300,'y':index*240,'width':480,'height':200,'text':'### 필드 / 정적 초기화\n\n[['+class_path(owner)[:-3]+'|'+CLASSES[owner]['name']+']]','color':'5'})
    for index,label in enumerate(sorted(runtime)):
        nodes.append({'id':uid(label,'runtime'),'type':'text','x':5900,'y':index*240,'width':480,'height':200,'text':'### 프레임워크 진입\n\n'+label,'color':'5'})
    for (source,target),sites in pairs.items():
        edges.append({'id':uid(source+'>'+target,'edge'),'fromNode':uid(source),'fromSide':'right','toNode':uid(target),'toSide':'left','toEnd':'arrow','label':'참조' if all(s['kind']=='reference' for s in sites) else '호출/생성'})
    for mid in sorted(selected):
        for label in FRAMEWORK[mid]:
            edges.append({'id':uid(label+'>'+mid,'framework'),'fromNode':uid(label,'runtime'),'fromSide':'right','toNode':uid(mid),'toSide':'left','toEnd':'arrow','label':'프레임워크','color':'5'})
    return {'nodes':nodes,'edges':edges}


def class_canvas():
    """클래스 노트를 바로 열 수 있는 전체 구조 Canvas를 만든다."""
    nodes,edges=[],[];y=0
    for f in FEATURES:
        owners=[o for o in CLASSES if feature(o)==f]
        if not owners:continue
        height=math.ceil(len(owners)/5)*390+100
        nodes.append({'id':uid(f,'feature'),'type':'group','x':0,'y':y,'width':3400,'height':height,'label':f,'color':COLORS[f]})
        for index,owner in enumerate(owners):
            nodes.append({'id':uid(owner),'type':'file','file':class_path(owner),'subpath':'#메서드 목록','x':30+(index%5)*670,'y':y+65+(index//5)*390,'width':640,'height':360})
        y+=height+230
    aggregates=defaultdict(int)
    for source,target in PAIRS:
        so=source.split('#')[0];to=METHODS[target]['owner']
        if so!=to:aggregates[(so,to)]+=1
    for (so,to),count in aggregates.items():
        edges.append({'id':uid(so+'>'+to,'classedge'),'fromNode':uid(so),'fromSide':'right','toNode':uid(to),'toSide':'left','toEnd':'arrow','label':str(count)+'개 메서드 호출 쌍'})
    return {'nodes':nodes,'edges':edges}



def main():
    """검증된 데이터로 탐색 문서와 전체 호출 그림을 순서대로 생성한다."""
    verify_sources()
    write_class_notes()
    guide.write_guide(sys.modules[__name__])
    save('01-Class-Map.canvas',class_canvas())
    save('02-All-Methods.canvas',method_canvas())
    for f in FEATURES:save('Canvases/'+f+'.canvas',method_canvas(f))
    svg_graph(set(CLASSES),Path('all-class-method-calls.svg'))
    print(json.dumps({'classNotes':len(CLASSES),'methods':len(METHODS),'internalPairs':len(PAIRS),
                      'canvasFiles':len(list(VAULT.rglob('*.canvas'))),'guideFlows':len(guide.FLOWS),'vault':str(VAULT)},ensure_ascii=False))


if __name__=='__main__':main()
