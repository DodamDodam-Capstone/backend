#!/usr/bin/env python3
"""메서드 누락·잘못된 호출선·끊어진 Obsidian 링크와 코드 사본 변형을 검사한다."""
from pathlib import Path
from urllib.parse import urlparse,parse_qs
import importlib.util
import json
import re
import sys
import textwrap
import xml.etree.ElementTree as ET

HERE=Path(__file__).resolve().parent
sys.dont_write_bytecode=True
spec=importlib.util.spec_from_file_location('call_vault',HERE/'generate_call_vault.py')
g=importlib.util.module_from_spec(spec)
spec.loader.exec_module(g)


def without_code(text):
    """Java 코드 안의 대괄호를 문서 링크로 잘못 해석하지 않는다."""
    return re.sub(r'```[\s\S]*?```','',text)


def wiki_targets(text):
    """현재 docs 보관소 기준으로 파일과 정확한 메서드 절을 검증한다."""
    count=0
    for match in re.finditer(r'\[\[([^\]|]+)(?:\|[^\]]*)?\]\]',without_code(text).replace('\\|','|')):
        destination,sep,anchor=match[1].partition('#')
        path=g.DOCS/destination
        if not path.suffix:path=path.with_suffix('.md')
        # 점으로 된 패키지 이름은 파일 확장자가 아니므로 .md를 다시 확인한다.
        if not path.exists() and Path(str(path)+'.md').exists():path=Path(str(path)+'.md')
        assert path.exists(),(destination,anchor)
        if sep:
            assert anchor in re.findall(r'^#{1,6} (.+)$',path.read_text(),re.M),(destination,anchor)
        count+=1
    return count


def verify_guide():
    """작은 안내 그림의 링크·단계 수와 Markdown 표의 별칭 구분자를 검증한다."""
    guide=g.guide
    references={ref for flow in guide.FLOWS for _,_,refs in flow['steps'] for ref in refs}
    references|={ref for flow in guide.FLOWS for _,_,ref in flow['failures']}
    references|={ref for info in guide.FEATURE_INFO.values() for ref in info[6]}
    for ref in references:guide.resolve(g,ref)
    namespace={'s':'http://www.w3.org/2000/svg'}
    count=0
    names=['00-Overview']+[f['id'] for f in guide.FLOWS]
    for name in names:
        svg=ET.parse(g.VAULT/'Assets'/f'{name}.svg')
        assert svg.getroot().get('width')=='1000',name
        links=svg.findall('.//s:a',namespace)
        assert len(links)==6,name
        for link in links:
            uri=link.attrib['href']
            assert uri.startswith('obsidian://open?'),uri
            target=parse_qs(urlparse(uri).query)['path'][0]
            path,sep,anchor=target.partition('#')
            assert Path(path).exists(),target
            if sep:assert anchor in re.findall(r'^#{1,6} (.+)$',Path(path).read_text(),re.M)
            count+=1
    for flow in guide.FLOWS:
        canvas=json.loads((g.VAULT/'Flows'/f"{flow['id']}.canvas").read_text())
        steps={n['id'] for n in canvas['nodes'] if n['id'].startswith('step-')}
        assert steps=={'step-'+str(i) for i in range(1,7)},flow['id']
        assert [(e['fromNode'],e['toNode']) for e in canvas['edges']]==[('step-'+str(i),'step-'+str(i+1)) for i in range(1,6)]
    for path in g.VAULT.rglob('*.md'):
        for line in without_code(path.read_text()).splitlines():
            if line.startswith('|'):
                assert not re.search(r'\[\[[^\]]*(?<!\\)\|[^\]]*\]\]',line),(path,line)
    return {'featureCards':6,'requestFlows':len(guide.FLOWS),'stepsPerFlow':6,
            'sourceMethodsReferenced':len(references),'svgDeepLinks':count,
            'svgWidth':1000,'tableAliasesEscaped':True}


def verify():
    """전체 SVG·Canvas·노트가 같은 검증된 호출 데이터를 표현하는지 확인한다."""
    g.verify_sources()
    notes=list((g.VAULT/'Classes').glob('*.md'))
    assert len(notes)==106
    definitions=0
    copies=0
    for mid,m in g.METHODS.items():
        note=(g.VAULT/g.class_path(m['owner'])).read_text()
        assert note.count('## '+g.heading(mid)+'\n')==1,mid
        definitions+=1
        if not m['generated']:
            source=(g.ROOT/m['file']).read_text().splitlines()
            body=textwrap.dedent('\n'.join(source[m['line']-1:m['endLine']]))
            assert '\n```java\n'+body+'\n```' in note,mid
            copies+=1
    assert definitions==566 and copies==271
    wikilinks=sum(wiki_targets(p.read_text()) for p in g.VAULT.rglob('*.md'))
    canvas_stats={}
    for path in sorted(g.VAULT.rglob('*.canvas')):
        data=json.loads(path.read_text())
        ids=[n['id'] for n in data['nodes']]
        assert len(ids)==len(set(ids)),path
        edgeids=[e['id'] for e in data['edges']]
        assert len(edgeids)==len(set(edgeids)),path
        for n in data['nodes']:
            assert n['type'] in ('group','file','text','link')
            assert all(isinstance(n[x],int) for x in ('x','y','width','height'))
            assert n['width']>0 and n['height']>0
            if n['type']=='text':wikilinks+=wiki_targets(n['text'])
            if n['type']=='file':
                linked=g.DOCS/n['file'];assert linked.exists(),n
                if n.get('subpath'):assert n['subpath'][1:] in re.findall(r'^#{1,6} (.+)$',linked.read_text(),re.M)
        for e in data['edges']:assert e['fromNode'] in ids and e['toNode'] in ids
        cards=[n for n in data['nodes'] if n['type']!='group']
        for index,a in enumerate(cards):
            for b in cards[index+1:]:
                overlap=min(a['x']+a['width'],b['x']+b['width'])>max(a['x'],b['x']) and min(a['y']+a['height'],b['y']+b['height'])>max(a['y'],b['y'])
                assert not overlap,(path.name,a['id'],b['id'])
        canvas_stats[path.name]={'nodes':len(ids),'edges':len(data['edges'])}
        if path.name=='02-All-Methods.canvas':
            assert {g.uid(mid) for mid in g.METHODS}<=set(ids)
            expected={g.uid(s+'>'+t,'edge') for s,t in g.PAIRS}
            assert expected<={e['id'] for e in data['edges']}
            assert len(data['edges'])==541+42
    svg=ET.parse(g.VAULT/'Assets/all-class-method-calls.svg')
    ns={'s':'http://www.w3.org/2000/svg'}
    groups={n.attrib.get('id'):n for n in svg.findall('.//s:g',ns)}
    assert all(g.uid(o) in groups for o in g.CLASSES)
    assert all(g.uid(s+'>'+t,'edge') in groups for s,t in g.PAIRS)
    uris=[]
    for n in svg.findall('.//s:a',ns):
        href=n.attrib.get('{http://www.w3.org/1999/xlink}href','')
        if href.startswith('obsidian://'):
            path=parse_qs(urlparse(href).query)['path'][0]
            note,sep,anchor=path.partition('#')
            assert Path(note).exists(),note
            if sep:assert anchor in re.findall(r'^#{1,6} (.+)$',Path(note).read_text(),re.M)
            uris.append(href)
    assert len(uris)==106+566
    # 원본이 바뀌었거나 문서 링크가 끊어진 경우 검사가 실제로 거부하는지 확인한다.
    first=g.DATA['snapshot']['files'][0]
    original=first['sha256'];first['sha256']='0'*64
    try:
        try:g.verify_sources()
        except AssertionError:pass
        else:raise RuntimeError('오래된 원본 검증이 거부되지 않았습니다.')
    finally:first['sha256']=original
    try:wiki_targets('[[analysis/missing-code-map-note]]')
    except AssertionError:pass
    else:raise RuntimeError('끊어진 링크가 거부되지 않았습니다.')
    guide=verify_guide()
    result={'checkedOn':'2026-11-17 Asia/Seoul','sourceHashes':'71 unchanged','classNotes':106,'methodDefinitions':definitions,'sourceCodeCopies':copies,'internalCallPairs':541,'frameworkEdges':42,'wikilinksChecked':wikilinks,'svgDeepLinks':len(uris),'canvasFiles':canvas_stats,'cardOverlap':False,'vaultRoot':'backend/docs','onboardingGuide':guide,'applicationUiVerified':False}
    (g.VAULT/'verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(result,ensure_ascii=False,indent=2))


if __name__=='__main__':verify()
