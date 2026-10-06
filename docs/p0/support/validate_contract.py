#!/usr/bin/env python3
"""Validate the delivered v3 design, not a running backend.

Requires PyYAML, openapi-spec-validator and openapi-schema-validator.
Run: python support/validate_contract.py
Use --api-only while the Markdown catalogs and diagrams are being generated.
No HTTP calls or database writes are performed.
"""
from copy import deepcopy
from pathlib import Path
import argparse
import json
import re
import warnings
import uuid
import hashlib
import xml.etree.ElementTree as ET
from urllib.parse import unquote
from datetime import date, datetime, timedelta, timezone

import yaml
from openapi_spec_validator import validate
from openapi_schema_validator import OAS30Validator, OAS30WriteValidator

warnings.filterwarnings('ignore', category=DeprecationWarning)
from jsonschema import RefResolver

ROOT = Path(__file__).resolve().parent.parent
DOC = yaml.safe_load((ROOT / 'Integrated_OpenAPI.yaml').read_text())
SCHEMAS = DOC['components']['schemas']
RESOLVER = RefResolver.from_schema(DOC)
KST = timezone(timedelta(hours=9))
# Only these source contracts change in v3; every other Part 1 schema stays exact.
SOURCE_SCHEMA_CHANGES = {
    'IssueEmailVerificationRequest': 'D16 adds the authenticated PIN_RESET purpose',
    'ChildView': 'F06 adds a separate required nickname; prior fields preserved',
    'CreateChildRequest': 'F06 adds required trimmed nickname; prior fields preserved',
    'ConversationView': 'F01-F04 adds CLOSING and immutable end cutoff/reason',
    'GuardianPinResetPendingRequest': 'D16 replaces the pending request with required PIN_RESET token',
}
COUNTS = {'schema_examples': 0, 'positive_cases': 0, 'negative_cases': 0,
          'semantic_positive_cases': 0, 'semantic_negative_cases': 0,
          'normalized_cases': 0, 'markdown_json_examples': 0,
          'markdown_schema_definitions': 0, 'db_json_examples': 0, 'db_schema_definitions': 0,
          'source_schemas_preserved': 0, 'v2_schemas_preserved': 0, 'source_hashes_verified': 0, 'source_hashes_unavailable': 0, 'source_schema_comparison_skipped': 0, 'local_links': 0, 'lifecycle_positive_cases': 0, 'lifecycle_negative_cases': 0,
          'db_dto_mappings': 0, 'rendered_diagrams': 0, 'external_local_links_unavailable': 0}


def strict_end_patterns(node):
    """Historical comparison accepts only the documented trailing-newline correction."""
    node = deepcopy(node)
    if isinstance(node, dict):
        if isinstance(node.get('pattern'), str) and node['pattern'].endswith('$'):
            node['pattern'] = node['pattern'][:-1] + r'(?![\s\S])'
        if node.get('pattern') == r'^audio/[^\s]+(?:\s*;.*)?(?![\s\S])':
            node['pattern'] = r'^audio/[^\s]+(?:[ \t]*;[^\r\n]*)?(?![\s\S])'
        return {k: strict_end_patterns(v) for k, v in node.items()}
    if isinstance(node, list): return [strict_end_patterns(v) for v in node]
    return node


def db_tables_from_markdown(content):
    tables = {}
    for heading in re.finditer(r'^### ([3-6])\.\d+ ([A-Za-z_]+)\s*$', content, re.M):
        table = heading[2]
        following = content[heading.end():]
        match = re.search(r'\n\| 컬럼 \|.*?\n((?:\|.*\n)+)', following)
        assert match, table
        columns = {}
        for line in match[1].splitlines()[1:]:
            cells = [c.strip().replace('\\|', '|') for c in re.split(r'(?<!\\)\|', line)[1:-1]]
            assert len(cells) == 5, (table, cells)
            name, kind, nullable, default, note = cells
            columns[name] = {'postgresType': kind, 'nullable': nullable == 'N',
                             'defaultOrCreationRule': default, 'keyAndMeaning': note}
        owner = 'Part 1' if heading[1] in ('3', '4') else ('Part 2' if heading[1] == '5' else 'Spring Session / Part 1 운영')
        tables[table] = {'table': table, 'owner': owner, 'columns': columns}
    assert len(tables) == 13 and sum(len(t['columns']) for t in tables.values()) == 115
    assert {'service_date', 'scheduled_end_at', 'end_requested_at', 'end_reason'} <= tables['conversations']['columns'].keys()
    assert 'nickname' in tables['children']['columns']
    assert 'topic_suggestions' not in tables['conversation_turns']['columns']
    return tables


def check_db_projection(table, value, tables):
    columns = tables[table]['columns']
    assert isinstance(value, dict) and set(value) <= columns.keys(), table
    for name, item in value.items():
        column = columns[name]
        if item is None:
            assert column['nullable'], (table, name)
            continue
        kind = column['postgresType'].lower()
        if kind == 'uuid':
            uuid.UUID(item)
        elif kind == 'date':
            date.fromisoformat(item)
        elif kind == 'timestamptz':
            assert datetime.fromisoformat(item).tzinfo is not None
        elif kind.endswith('[]'):
            assert isinstance(item, list)
            assert all(isinstance(v, str) for v in item)
        elif kind in ('integer', 'int', 'bigint'):
            assert isinstance(item, int) and not isinstance(item, bool)
        elif kind == 'boolean':
            assert isinstance(item, bool)
        else:
            assert isinstance(item, str), (table, name, kind)


def example_schema(fixture):
    if 'error' in fixture:
        return 'ActiveConversationExistsError' if 'activeConversationId' in fixture['error'] else 'ApiError'
    if 'headerName' in fixture: return 'CsrfTokenResponse'
    if 'data' not in fixture: return 'TurnView'
    data = fixture['data']
    if data.get('status') == 'CLOSING': return 'EndPendingReceiptResponse'
    for field, name in [('conversation', 'ConversationDetailResponse'), ('turn', 'ChildTurnResultResponse'),
                        ('statusUrl', 'TurnAcceptedResponse'), ('endedAt', 'EndReceiptResponse'),
                        ('processingTurnId', 'ResumeConversationResultResponse'),
                        ('conversationId', 'StartConversationResultResponse'), ('page', 'ConversationPageResponse'),
                        ('items', 'ChildrenListResponse'), ('child', 'ChildHomeResponse'),
                        ('email', 'AccountViewResponse'), ('accountId', 'SignupResultResponse'),
                        ('childId', 'ChildViewResponse'), ('challengeId', 'VerificationIssueResponse'),
                        ('verificationToken', 'VerificationGrantResponse'), ('guardianUnlockedUntil', 'GuardianUnlockResponse')]:
        if field in data: return name
    raise AssertionError(('Unknown Markdown fixture', fixture))


def deref(obj):
    while isinstance(obj, dict) and '$ref' in obj:
        ref = obj['$ref']
        assert ref.startswith('#/'), ref
        obj = DOC
        for key in ref[2:].split('/'):
            obj = obj[key.replace('~1', '/').replace('~0', '~')]
    return obj


def accepts(schema, value):
    # The generic validator skips required writeOnly fields; requests must not.
    validator_type = OAS30WriteValidator if schema.get('$ref', '').endswith('Request') else OAS30Validator
    validator = validator_type(schema, resolver=RESOLVER,
                               format_checker=validator_type.FORMAT_CHECKER)
    return list(validator.iter_errors(value))


def expect(name, value, valid=True):
    errors = accepts({'$ref': '#/components/schemas/' + name}, value)
    assert bool(errors) != valid, (name, valid, value, [str(e) for e in errors])
    COUNTS['positive_cases' if valid else 'negative_cases'] += 1


def semantic(value):
    """Cross-field example invariants that OAS 3.0 cannot fully express."""
    if isinstance(value, list):
        for item in value:
            semantic(item)
    elif isinstance(value, dict):
        for start, end in [('createdAt', 'completedAt'), ('startedAt', 'endedAt')]:
            if value.get(start) is not None and value.get(end) is not None:
                assert datetime.fromisoformat(value[end]) >= datetime.fromisoformat(value[start]), (start, end)
        if 'startedAt' in value:
            started = datetime.fromisoformat(value['startedAt']).astimezone(KST)
            midnight = started.replace(hour=0, minute=0, second=0, microsecond=0)
            assert started >= midnight + timedelta(hours=8), 'daily conversation starts at/after 08:00'
            if 'endRequestedAt' in value:
                cutoff = value['endRequestedAt']
                if value['status'] == 'ACTIVE':
                    assert cutoff is None and value['endReason'] is None and value['endedAt'] is None
                else:
                    cutoff = datetime.fromisoformat(cutoff)
                    assert started <= cutoff <= midnight + timedelta(days=1), 'end boundary range'
                    if value['endReason'] == 'MANUAL':
                        assert cutoff < midnight + timedelta(days=1), 'manual before midnight'
                    else:
                        assert value['endReason'] == 'MIDNIGHT' and cutoff == midnight + timedelta(days=1), 'midnight cutoff'
                    if value['status'] == 'ENDED':
                        assert datetime.fromisoformat(value['endedAt']) >= cutoff, 'end before cutoff'
                    else:
                        assert value['status'] == 'CLOSING' and value['endedAt'] is None
            if 'serviceDate' in value:
                assert date.fromisoformat(value['serviceDate']) == started.date(), 'conversation service date'
                assert datetime.fromisoformat(value['scheduledEndAt']) == midnight + timedelta(days=1), 'scheduled next KST midnight'
        if {'serverTime', 'opensAt', 'closesAt', 'canEnter'} <= value.keys():
            now = datetime.fromisoformat(value['serverTime']).astimezone(KST)
            midnight = now.replace(hour=0, minute=0, second=0, microsecond=0)
            opens = midnight + timedelta(hours=8)
            assert value['timeZone'] == 'Asia/Seoul'
            assert date.fromisoformat(value['serviceDate']) == now.date(), 'Home service date'
            assert datetime.fromisoformat(value['opensAt']) == opens, 'Home opens at 08:00'
            assert datetime.fromisoformat(value['closesAt']) == midnight + timedelta(days=1), 'Home closes at midnight'
            if now < opens:
                assert value['canEnter'] is False and value['reason'] == 'OUTSIDE_SERVICE_HOURS'
                assert datetime.fromisoformat(value['nextOpensAt']) == opens, 'next opening is today 08:00'
            elif value['canEnter']:
                assert value['reason'] is None and value['nextOpensAt'] is None
            else:
                assert value['reason'] == 'CONVERSATION_CLOSING' and value['nextOpensAt'] is None
        if {'availability', 'menus', 'activeConversationId'} <= value.keys():
            menus = value['menus']
            assert [m['code'] for m in menus] == ['TALK', 'PLAY'], 'Home menu order/uniqueness'
            assert menus[0]['status'] == ('AVAILABLE' if value['availability']['canEnter'] else 'UNAVAILABLE'), 'TALK availability'
            assert menus[1]['status'] == 'COMING_SOON'
            assert value['availability']['canEnter'] or value['activeConversationId'] is None, 'unavailable Home cannot expose active ID'
        if 'statusUrl' in value and 'turnId' in value:
            assert value['statusUrl'].rsplit('/', 1)[-1] == value['turnId'], 'status URL turnId'
        if value.get('audio') is not None and 'turn' in value:
            assert value['audio']['url'].rsplit('/', 2)[-2] == value['turn']['turnId'], 'audio URL turnId'
        if 'turns' in value:
            seq = [item['sequence'] for item in value['turns']]
            assert seq == sorted(set(seq)), 'turn sequence order/uniqueness'
            assert len({item['turnId'] for item in value['turns']}) == len(seq), 'turn ID uniqueness'
            for prev, current in zip(value['turns'], value['turns'][1:]):
                assert prev.get('completedAt') is not None, 'no later turn during PROCESSING'
                assert datetime.fromisoformat(current['createdAt']) >= datetime.fromisoformat(prev['completedAt']), 'overlapping processing turns'
            if 'processingTurnId' in value:
                ids = [t['turnId'] for t in value['turns'] if t['status'] == 'PROCESSING']
                assert len(ids) <= 1 and value['processingTurnId'] == (ids[0] if ids else None)
            if 'nextAfterSequence' in value:
                assert value['nextAfterSequence'] == (seq[-1] if value['hasNext'] and seq else None)
                assert not value['hasNext'] or bool(seq)
        if {'items', 'size', 'page'} <= value.keys():
            assert len(value['items']) <= value['size']
        for child in value.values():
            semantic(child)


def expect_semantic(value, valid=True):
    try:
        semantic(value)
    except AssertionError:
        assert not valid, value
    else:
        assert valid, ('Semantic fixture unexpectedly accepted', value)
    COUNTS['semantic_positive_cases' if valid else 'semantic_negative_cases'] += 1


def walk_examples(node):
    if isinstance(node, list):
        for item in node:
            walk_examples(item)
    elif isinstance(node, dict):
        if 'schema' in node:
            examples = []
            if 'example' in node:
                examples.append(node['example'])
            for example in node.get('examples', {}).values():
                example = deref(example)
                if 'value' in example:
                    examples.append(example['value'])
            for value in examples:
                errors = accepts(node['schema'], value)
                assert not errors, [str(e) for e in errors]
                semantic(value)
                COUNTS['schema_examples'] += 1
        for key, value in node.items():
            if key == '$ref':
                deref(node)
            else:
                walk_examples(value)


def lifecycle(trace):
    """Contract trace invariants; no claim about application lock implementation."""
    cutoff = datetime.fromisoformat(trace['cutoff'])
    ended = datetime.fromisoformat(trace['endedAt'])
    assert ended >= cutoff
    assert len(set(trace['cutoffObservations'])) == 1 and trace['cutoffObservations'][0] == trace['cutoff']
    assert len(set(trace['reasonObservations'])) == 1 and trace['reasonObservations'][0] in ('MANUAL', 'MIDNIGHT')
    for turn in trace['turns']:
        assert datetime.fromisoformat(turn['acceptedAt']) < cutoff, 'new input after closing'
        assert turn['deadlineAfter'] == turn['originalDeadline'], 'drain deadline changed'
        assert turn['terminalAfterLateCallback'] == turn['terminal'], 'late callback overwrites terminal'
        assert datetime.fromisoformat(turn['completedAt']) <= ended, 'ended before drain'
    assert all(not event['exposed'] for event in trace['childContentAfterCutoff']), 'content while closing'
    for link in trace['links']:
        eligible = link['validAtEnd'] and datetime.fromisoformat(link['boundAt']) < cutoff
        assert link['canSeePending'] == (eligible and link['loginValid'])
        expiry = ended + timedelta(seconds=600)
        assert link['recoveryUntil'] == (expiry.isoformat() if eligible else None)
        assert link['canSeeEnded'] == (eligible and link['loginValid'] and datetime.fromisoformat(link['observedAt']) < expiry)
    for snapshot in trace['snapshots']:
        unfinished = [row['childId'] for row in snapshot if row['status'] in ('ACTIVE', 'CLOSING')]
        assert len(unfinished) == len(set(unfinished)), 'more than one unfinished per child'
    assert trace['summaryCalls'] == int(trace['hasAllowedInput']), 'EMPTY calls AI or summary retried'
    assert trace['summaryStatus'] == ('PENDING' if trace['hasAllowedInput'] else 'EMPTY')
    assert trace['summaryConversationId'] == trace['endedConversationId'], 'summary crosses conversations'
    assert trace['greetingCalls'] == trace['greetingTurnCount'] == 0, 'fixed greeting treated as AI/turn'


def lifecycle_cases():
    valid = {'cutoff': '2026-10-03T13:10:00+09:00', 'endedAt': '2026-10-03T13:10:03+09:00',
             'cutoffObservations': ['2026-10-03T13:10:00+09:00'] * 3,
             'reasonObservations': ['MANUAL'] * 3,
             'turns': [{'acceptedAt': '2026-10-03T13:09:59+09:00', 'originalDeadline': '2026-10-03T13:11:00+09:00',
                        'deadlineAfter': '2026-10-03T13:11:00+09:00', 'completedAt': '2026-10-03T13:10:03+09:00',
                        'terminal': 'SUCCEEDED', 'terminalAfterLateCallback': 'SUCCEEDED'}],
             'childContentAfterCutoff': [{'path': path, 'exposed': False} for path in ['resume', 'turn-post', 'turn-get', 'request-get', 'audio']],
             'links': [{'validAtEnd': True, 'loginValid': True, 'boundAt': '2026-10-03T13:00:00+09:00', 'canSeePending': True,
                        'recoveryUntil': '2026-10-03T13:20:03+09:00', 'observedAt': '2026-10-03T13:20:02+09:00', 'canSeeEnded': True},
                       {'validAtEnd': True, 'loginValid': True, 'boundAt': '2026-10-03T13:00:00+09:00', 'canSeePending': True,
                        'recoveryUntil': '2026-10-03T13:20:03+09:00', 'observedAt': '2026-10-03T13:20:03+09:00', 'canSeeEnded': False},
                       {'validAtEnd': True, 'loginValid': True, 'boundAt': '2026-10-03T13:10:00+09:00', 'canSeePending': False,
                        'recoveryUntil': None, 'observedAt': '2026-10-03T13:10:04+09:00', 'canSeeEnded': False},
                       {'validAtEnd': True, 'loginValid': False, 'boundAt': '2026-10-03T13:00:00+09:00', 'canSeePending': False,
                        'recoveryUntil': '2026-10-03T13:20:03+09:00', 'observedAt': '2026-10-03T13:10:04+09:00', 'canSeeEnded': False}],
             'snapshots': [[{'childId':'child','status':'CLOSING','serviceDate':'2026-10-03'}],
                           [{'childId':'child','status':'ENDED','serviceDate':'2026-10-03'},
                            {'childId':'child','status':'ACTIVE','serviceDate':'2026-10-03'}]],
             'hasAllowedInput': True, 'summaryCalls': 1, 'summaryStatus': 'PENDING',
             'summaryConversationId': 'old', 'endedConversationId': 'old', 'greetingCalls': 0, 'greetingTurnCount': 0}
    def check(trace, good=True):
        try: lifecycle(trace)
        except AssertionError: assert not good, trace
        else: assert good, 'invalid lifecycle trace accepted'
        COUNTS['lifecycle_positive_cases' if good else 'lifecycle_negative_cases'] += 1
    check(valid)
    empty = deepcopy(valid); empty.update(turns=[], hasAllowedInput=False, summaryCalls=0, summaryStatus='EMPTY', endedAt=valid['cutoff'])
    empty['links'] = []; check(empty)
    midnight = deepcopy(empty)
    midnight.update(cutoff='2026-10-04T00:00:00+09:00', endedAt='2026-10-04T00:00:03+09:00',
                    cutoffObservations=['2026-10-04T00:00:00+09:00'] * 3, reasonObservations=['MIDNIGHT'] * 3)
    check(midnight)
    # Immutable cutoff/first reason, drain ordering, equality and invalid-login restoration.
    mutations = [lambda x: x['cutoffObservations'].__setitem__(1, '2026-10-03T13:11:00+09:00'),
                 lambda x: x['reasonObservations'].__setitem__(1, 'MIDNIGHT'),
                 lambda x: x['turns'][0].update(acceptedAt=x['cutoff']),
                 lambda x: x['turns'][0].update(deadlineAfter='2026-10-03T13:12:00+09:00'),
                 lambda x: x['turns'][0].update(terminalAfterLateCallback='FAILED'),
                 lambda x: x.update(endedAt='2026-10-03T13:10:01+09:00'),
                 lambda x: x['links'][0].update(recoveryUntil='2026-10-03T13:21:03+09:00'),
                 lambda x: x['links'][1].update(canSeeEnded=True),
                 lambda x: x['links'][2].update(canSeePending=True),
                 lambda x: x['links'][3].update(canSeeEnded=True),
                 lambda x: x['snapshots'][0].append({'childId':'child','status':'ACTIVE','serviceDate':'2026-10-04'}),
                 lambda x: x.update(summaryCalls=2), lambda x: x.update(summaryConversationId='new'),
                 lambda x: x.update(greetingCalls=1), lambda x: x.update(greetingTurnCount=1)]
    for mutation in mutations:
        bad = deepcopy(valid); mutation(bad); check(bad, False)
    for i in range(5):
        bad = deepcopy(valid); bad['childContentAfterCutoff'][i]['exposed'] = True; check(bad, False)
    bad = deepcopy(empty); bad['summaryCalls'] = 1; check(bad, False)


def main(api_only=False):
    validate(DOC)
    samples = ['user@example.com', 'dodam', '012345', '0123',
               '11111111-1111-4111-8111-111111111111.' + 'A' * 43,
               '2026-10-03T14:01:00+09:00', 'audio/wav',
               '/api/v1/children/11111111-1111-4111-8111-111111111111/conversations/22222222-2222-4222-8222-222222222222/turns/33333333-3333-4333-8333-333333333333',
               '/api/v1/children/11111111-1111-4111-8111-111111111111/conversations/22222222-2222-4222-8222-222222222222/turns/33333333-3333-4333-8333-333333333333/audio']
    def anchored(node):
        if isinstance(node, dict):
            pattern = node.get('pattern')
            if pattern and pattern.startswith('^'):
                assert pattern.endswith(r'(?![\s\S])'), pattern
                valid = next(x for x in samples if not accepts(node, x))
                COUNTS['positive_cases'] += 1
                for suffix in ['\n', '\r\n', '\r', '\u2028', '\u2029']:
                    assert accepts(node, valid + suffix), pattern
                    COUNTS['negative_cases'] += 1
            for value in node.values(): anchored(value)
        elif isinstance(node, list):
            for value in node: anchored(value)
    anchored(SCHEMAS)
    content_type = SCHEMAS['TemporaryAudio']['properties']['contentType']
    assert not accepts(content_type, 'audio/wav;codec=pcm')
    COUNTS['positive_cases'] += 1
    for invalid in ['audio/wav\r\n;codec=pcm', 'audio/wav;codec=pcm\r\nX-Test: bad']:
        assert accepts(content_type, invalid)
        COUNTS['negative_cases'] += 1
    for source in DOC['x-source-manifest']:
        file = ROOT / 'sources' / source['file']
        if file.exists():
            assert hashlib.sha256(file.read_bytes()).hexdigest() == source['sha256'], file
            COUNTS['source_hashes_verified'] += 1
        else:
            COUNTS['source_hashes_unavailable'] += 1
    paths = DOC['paths']
    operations = [(path, method, op) for path, methods in paths.items()
                  for method, op in methods.items() if method in ('get', 'post')]
    assert len(paths) == 24 and len(operations) == 26 and len(SCHEMAS) == 61
    assert len({op['operationId'] for _, _, op in operations}) == 26
    assert sum(op['x-owner'] == 'Part 1' for _, _, op in operations) == 18
    assert sum(op['x-owner'] == 'Part 2' for _, _, op in operations) == 8
    for path, method, op in operations:
        assert all(key in op for key in ['x-owner', 'x-task-id', 'x-tables-read', 'x-tables-write']), op['operationId']
        params = [deref(p) for p in op.get('parameters', [])]
        assert {p['name'] for p in params if p['in'] == 'path'} == set(re.findall(r'{([^}]+)}', path))
        assert all(p.get('required') for p in params if p['in'] == 'path')
        if method == 'post':
            assert any(p['name'] == 'X-XSRF-TOKEN' and p.get('required') for p in params)
        for status, response in op['responses'].items():
            response = deref(response)
            assert {'Cache-Control', 'X-Request-ID'} <= response.get('headers', {}).keys()
            if status in ('204', '302'):
                assert not response.get('content')
    raw_contract = json.dumps(DOC, ensure_ascii=False)
    for obsolete in ['PREVIOUS_CONVERSATION_CLOSING', 'DAILY_CONVERSATION_CLOSED', 'CONVERSATION_NOT_ENDED']:
        assert obsolete not in raw_contract, obsolete
    walk_examples(DOC)

    # Preserve source operations and unchanged constraints; document the D16 and F01-F06 changes.
    original = ROOT / 'sources' / next(s['file'] for s in DOC['x-source-manifest'] if Path(s['file']).name == 'part1-api.yaml')
    if not original.exists():
        COUNTS['source_schema_comparison_skipped'] = 1
    if original.exists():
        source = yaml.safe_load(original.read_text())
        originals = strict_end_patterns(source['components']['schemas'])
        assert len(originals) == 31
        assert set(originals) - SCHEMAS.keys() == {'GuardianPinResetPendingRequest'}
        assert SOURCE_SCHEMA_CHANGES.keys() <= originals.keys()
        assert 'GuardianPinResetRequest' in SCHEMAS
        def constraints(node):
            if isinstance(node, list):
                return [constraints(v) for v in node]
            if isinstance(node, dict):
                return {k: constraints(v) for k, v in node.items()
                        if not k.startswith('x-') and k not in ('description', 'title', 'example', 'examples')}
            return node
        for name, schema in originals.items():
            if name in SOURCE_SCHEMA_CHANGES:
                continue
            assert constraints(schema) == constraints(SCHEMAS[name]), name
            COUNTS['source_schemas_preserved'] += 1
        for name in ['ChildView', 'CreateChildRequest']:
            changed = deepcopy(SCHEMAS[name])
            del changed['properties']['nickname']
            changed['required'].remove('nickname')
            assert constraints(changed) == constraints(originals[name]), name
        unchanged = set(originals['ConversationView']['properties']) - {'status'}
        for field in unchanged:
            assert constraints(originals['ConversationView']['properties'][field]) == constraints(SCHEMAS['ConversationView']['properties'][field]), field
        assert constraints(originals['ConversationView']['allOf'][1]) == constraints(SCHEMAS['ConversationView']['allOf'][1])
        issue = deepcopy(originals['IssueEmailVerificationRequest'])
        issue['oneOf'][1]['properties']['purpose']['enum'].append('PIN_RESET')
        assert constraints(issue) == constraints(SCHEMAS['IssueEmailVerificationRequest'])
        reset = SCHEMAS['GuardianPinResetRequest']
        assert constraints(reset['properties']['newPin']) == constraints(originals['GuardianPinResetPendingRequest']['properties']['newPin'])
        assert constraints(reset['properties']['verificationToken']) == constraints(SCHEMAS['GuardianPinSetupRequest']['properties']['verificationToken'])
        assert set(reset['required']) == {'newPin', 'verificationToken'}
        assert reset['additionalProperties'] is False
        for path, methods in source['paths'].items():
            for method, op in methods.items():
                if method in ('get', 'post'):
                    assert op['operationId'] == paths[path][method]['operationId']
                    assert set(op['responses']) <= paths[path][method]['responses'].keys()

    previous = ROOT.parent / 'p0-final-design-2026-10-02' / 'Integrated_OpenAPI.yaml'
    if previous.exists():
        previous_schemas = strict_end_patterns(yaml.safe_load(previous.read_text())['components']['schemas'])
        changed = {'ChildView', 'CreateChildRequest', 'ChildHome', 'ConversationView', 'ConversationAvailability'}
        def structural(node):
            if isinstance(node, list): return [structural(v) for v in node]
            if isinstance(node, dict):
                return {k: structural(v) for k, v in node.items()
                        if not k.startswith('x-') and k not in ('description', 'title', 'example', 'examples')}
            return node
        for name, schema in previous_schemas.items():
            if name not in changed:
                assert structural(schema) == structural(SCHEMAS[name]), name
                COUNTS['v2_schemas_preserved'] += 1

    turn = {'turnId': '55555555-5555-4555-8555-555555555555', 'sequence': 1,
            'status': 'SUCCEEDED', 'childText': '안녕', 'childTextVisibility': 'VISIBLE',
            'replyText': '반가워', 'replyTextVisibility': 'VISIBLE',
            'createdAt': '2026-10-02T13:00:00+09:00',
            'completedAt': '2026-10-02T13:00:03+09:00', 'errorCode': None}
    expect('TurnView', turn)
    for field, value in [('sequence', 0), ('completedAt', None), ('errorCode', 'FAIL'),
                         ('childText', None), ('replyTextVisibility', 'OMITTED'),
                         ('createdAt', '2026-10-02T04:00:00Z'), ('unlisted', True)]:
        bad = dict(turn, **{field: value}); expect('TurnView', bad, False)
    processing = dict(turn, status='PROCESSING', childText=None, replyText=None,
                      childTextVisibility='OMITTED', replyTextVisibility='OMITTED', completedAt=None)
    expect('TurnView', processing)
    expect('TurnView', dict(processing, completedAt=turn['completedAt']), False)
    failed = dict(turn, status='FAILED', errorCode='AI_TIMEOUT')
    expect('TurnView', failed)
    expect('TurnView', dict(failed, errorCode=None), False)
    expect('TurnView', dict(failed, errorCode=''), False)
    for visibility, value in [('VISIBLE', '허용'), ('REDACTED', '허용 가공'), ('OMITTED', None)]:
        expect('TurnView', dict(turn, childTextVisibility=visibility, childText=value))
    expect('ChildTurnResult', {'turn': processing, 'audio': None, 'topicSuggestions': []})
    expect('ChildTurnResult', {'turn': processing, 'audio': None, 'topicSuggestions': ['처리 중 노출']}, False)
    for name, state in [('ProcessingChildTurnResult', processing), ('SucceededChildTurnResult', turn),
                        ('FailedChildTurnResult', failed)]:
        fixture = {'turn': state, 'audio': None, 'topicSuggestions': []}
        expect(name, fixture)
        expect(name, dict(fixture, topicSuggestions=['P0 추천 금지']), False)
        expect(name, dict(fixture, topicSuggestions=None), False)
    expect('TurnAccepted', {'turnId': turn['turnId'], 'clientRequestId': turn['turnId'],
                           'status': 'SUCCEEDED', 'statusUrl': '/wrong'}, False)
    def response_case(operation_id, status, body, valid):
        operation = next(o for _, _, o in operations if o['operationId'] == operation_id)
        schema = deref(operation['responses'][status])['content']['application/json']['schema']
        assert bool(accepts(schema, body)) != valid, (operation_id, status, body)
        COUNTS['positive_cases' if valid else 'negative_cases'] += 1
    pending_body = {'data': {'turn': processing, 'audio': None, 'topicSuggestions': []}}
    failed_body = {'data': {'turn': failed, 'audio': None, 'topicSuggestions': []}}
    response_case('submitVoiceTurn', '201', failed_body, False)
    response_case('submitVoiceTurn', '200', pending_body, False)
    response_case('submitVoiceTurn', '202', pending_body, False)
    response_case('submitVoiceTurn', '200', failed_body, True)
    response_case('getVoiceTurn', '200', pending_body, True)

    conversation = {'conversationId': '33333333-3333-4333-8333-333333333333',
                    'childId': '22222222-2222-4222-8222-222222222222', 'status': 'ACTIVE',
                    'startedAt': turn['createdAt'], 'endRequestedAt': None, 'endReason': None, 'endedAt': None, 'title': None,
                    'topic': None, 'summary': None, 'summaryStatus': 'NOT_STARTED'}
    expect('ConversationView', conversation)
    expect('ConversationView', dict(conversation, endedAt=turn['completedAt']), False)
    expect('ConversationView', dict(conversation, summaryStatus='PENDING'), False)
    ended = dict(conversation, status='ENDED', endRequestedAt='2026-10-03T00:00:00+09:00', endReason='MIDNIGHT', endedAt='2026-10-03T00:00:03+09:00', summaryStatus='FAILED')
    expect('ConversationView', ended)
    closing = dict(conversation, status='CLOSING', endRequestedAt='2026-10-02T13:01:00+09:00', endReason='MANUAL')
    expect('ConversationView', closing)
    expect_semantic(closing)
    manual = dict(closing, status='ENDED', endedAt='2026-10-02T13:01:03+09:00', summaryStatus='EMPTY')
    expect('ConversationView', manual)
    expect_semantic(manual)
    for field, value in [('endRequestedAt', None), ('endReason', None), ('endedAt', turn['completedAt']), ('summaryStatus', 'PENDING')]:
        expect('ConversationView', dict(closing, **{field: value}), False)
    for field, value in [('endRequestedAt', turn['createdAt']), ('endReason', 'MANUAL')]:
        expect('ConversationView', dict(conversation, **{field: value}), False)
    expect('ConversationView', dict(manual, endReason='UNKNOWN'), False)
    pending_end = {'conversationId': conversation['conversationId'], 'status': 'CLOSING'}
    expect('EndPendingReceipt', pending_end)
    for field, value in [('endedAt', None), ('summaryStatus', 'NOT_STARTED'), ('status', 'ENDED'), ('turns', [])]:
        expect('EndPendingReceipt', dict(pending_end, **{field: value}), False)
    for key in pending_end:
        missing = dict(pending_end); del missing[key]
        expect('EndPendingReceipt', missing, False)
    response_case('endChildConversation', '202', {'data': pending_end}, True)
    response_case('endChildConversation', '200', {'data': pending_end}, False)
    receipt = {k: manual[k] for k in ['conversationId', 'status', 'endedAt', 'summaryStatus']}
    response_case('endChildConversation', '200', {'data': receipt}, True)
    response_case('endChildConversation', '202', {'data': receipt}, False)
    end_op = next(o for _, _, o in operations if o['operationId'] == 'endChildConversation')
    assert '409' not in end_op['responses'] and 'requestBody' not in end_op
    assert end_op['responses']['202']['headers']['Retry-After']['schema']['minimum'] == 1

    expect('ConversationView', dict(ended, summary='미완료 노출'), False)
    expect('ConversationView', dict(ended, summaryStatus='READY'), False)
    ready = dict(ended, summaryStatus='READY', title='제목', topic='주제', summary='요약')
    expect('ConversationView', ready)
    page = {'conversation': ended, 'turns': [turn], 'hasNext': False, 'nextAfterSequence': None}
    expect('ConversationDetail', page)
    expect('ConversationDetail', dict(page, hasNext=True), False)
    expect('ConversationDetail', dict(page, conversation=conversation), False)
    expect('ConversationDetail', dict(page, conversation=closing), False)
    expect('ConversationPage', {'items': [closing], 'page': 0, 'size': 20, 'hasNext': False}, False)
    expect('EndReceipt', {k: ended[k] for k in ['conversationId', 'status', 'endedAt', 'summaryStatus']})
    expect('EndReceipt', dict({k: ended[k] for k in ['conversationId', 'status', 'endedAt', 'summaryStatus']}, summary='leak'), False)

    for schema_name, fixture in [('TurnView', turn), ('ConversationView', conversation),
                                 ('EndReceipt', {k: ended[k] for k in ['conversationId', 'status', 'endedAt', 'summaryStatus']})]:
        for key in fixture:
            bad = deepcopy(fixture); del bad[key]
            expect(schema_name, bad, False)
    pin = {'pin': '0000'}
    expect('GuardianUnlockRequest', pin)
    for value in [0, '000', '１２３４', ' 0000', None]:
        expect('GuardianUnlockRequest', {'pin': value}, False)
    expect('GuardianPinSetupRequest', pin)  # business absence is 403, not structural 400
    expect('GuardianPinSetupRequest', dict(pin, verificationToken=None), False)
    reset = {'newPin': '0456', 'verificationToken': '88888888-8888-4888-8888-888888888888.' + 'A' * 43}
    expect('GuardianPinResetRequest', reset)
    for value in [{'newPin': '0456'}, dict(reset, verificationToken=None),
                  dict(reset, verificationToken='bad'), dict(reset, newPin='456'),
                  dict(reset, pin='0456')]:
        expect('GuardianPinResetRequest', value, False)  # required token absence is structural 400
    for purpose in ['PIN_SETUP', 'PIN_RESET']:
        expect('IssueEmailVerificationRequest', {'purpose': purpose})
        expect('IssueEmailVerificationRequest', {'purpose': purpose, 'email': None}, False)
    expect('IssueEmailVerificationRequest', {'purpose': 'PIN_CHANGE'}, False)
    expect('StartConversationRequest', {'clientRequestId': turn['turnId']})
    for value in [{}, {'clientRequestId': None}, {'clientRequestId': 'bad'},
                  {'clientRequestId': turn['turnId'], 'accountId': turn['turnId']}]:
        expect('StartConversationRequest', value, False)

    conflict = {'error': {'code': 'ACTIVE_CONVERSATION_EXISTS', 'message': '진행 중',
                          'requestId': turn['turnId'], 'fields': [],
                          'activeConversationId': conversation['conversationId']}}
    conflict_schema = deref(paths['/api/v1/children/{childId}/conversations']['post']['responses']['409'])['content']['application/json']['schema']
    assert not accepts(conflict_schema, conflict)
    COUNTS['positive_cases'] += 1
    del conflict['error']['activeConversationId']
    assert accepts(conflict_schema, conflict), 'ACTIVE conflict must include ID in the retained technical contract'
    COUNTS['negative_cases'] += 1
    for code in ['CONVERSATION_CLOSING', 'CONVERSATION_ENDED']:
        conflict['error']['code'] = code
        response_case('startChildConversation', '409', conflict, True)
    for code in ['PREVIOUS_CONVERSATION_CLOSING', 'DAILY_CONVERSATION_CLOSED', 'CONVERSATION_NOT_ENDED']:
        conflict['error']['code'] = code
        response_case('startChildConversation', '409', conflict, False)

    # x-normalized-schema is a service-side constraint, not an OAS keyword.
    child_schema = SCHEMAS['CreateChildRequest']['properties']
    for field, value, valid in [('name', '가나다라마', True), ('name', '', False),
                              ('name', '가나다라마바', False), ('interests', ['놀이'], True),
                              ('interests', ['놀이', '놀이'], False), ('interests', [''], False),
                              ('nickname', '별' * 20, True), ('nickname', '별' * 21, False),
                              ('nickname', '🙂' * 20, True), ('nickname', '', False), ('nickname', None, False)]:
        assert bool(accepts(child_schema[field]['x-normalized-schema'], value)) != valid
        COUNTS['normalized_cases'] += 1

    child = {'name': '도담', 'nickname': ' 도담이 ', 'birthDate': '2022-01-01', 'gender': 'MALE', 'characterId': 'dodam'}
    expect('CreateChildRequest', child)
    for value in [None, '', '  ']:
        expect('CreateChildRequest', dict(child, nickname=value), False)
    missing = dict(child); del missing['nickname']
    expect('CreateChildRequest', missing, False)
    for raw, valid in [('  별  ', True), ('  ' + '별'*20 + '  ', True), (' ' * 3, False), ('별'*21, False)]:
        assert bool(accepts(child_schema['nickname']['x-normalized-schema'], raw.strip())) != valid
        COUNTS['normalized_cases'] += 1

    start = {'conversationId': conversation['conversationId'], 'status': 'ACTIVE',
             'startedAt': turn['createdAt'], 'serviceDate': '2026-10-02',
             'scheduledEndAt': '2026-10-03T00:00:00+09:00'}
    resume = dict(start, turns=[turn], processingTurnId=None)
    for name, fixture in [('StartConversationResult', start), ('ResumeConversationResult', resume)]:
        expect(name, fixture)
        expect_semantic(fixture)
        for key in ['serviceDate', 'scheduledEndAt']:
            missing = dict(fixture); del missing[key]
            expect(name, missing, False)
        expect_semantic(dict(fixture, serviceDate='2026-10-01'), False)
        expect_semantic(dict(fixture, scheduledEndAt='2026-10-02T23:59:59+09:00'), False)
    expect_semantic(dict(start, startedAt='2026-10-02T08:00:00+09:00'))
    expect_semantic(dict(start, startedAt='2026-10-02T07:59:59+09:00'), False)
    expect_semantic(ended)
    sequential = {'turns': [turn, dict(turn, turnId='77777777-7777-4777-8777-777777777777', sequence=2, createdAt=turn['completedAt'])]}
    expect_semantic(sequential)
    overlapping = deepcopy(sequential); overlapping['turns'][1]['createdAt'] = turn['createdAt']
    expect_semantic(overlapping, False)
    expect_semantic(dict(ended, endedAt='2026-10-03T00:00:00+09:00'))
    expect_semantic(dict(ended, endedAt='2026-10-02T23:59:59+09:00'), False)
    expect_semantic(dict(manual, endRequestedAt='2026-10-03T00:00:00+09:00', endedAt='2026-10-03T00:00:01+09:00'), False)
    expect_semantic(dict(manual, endReason='MIDNIGHT'), False)
    expect_semantic(dict(manual, endRequestedAt='2026-10-02T12:59:59+09:00'), False)
    expect_semantic(dict(manual, endedAt='2026-10-02T13:00:59+09:00'), False)


    availability = {'timeZone': 'Asia/Seoul', 'serverTime': '2026-10-02T08:00:00+09:00',
                    'serviceDate': '2026-10-02', 'opensAt': '2026-10-02T08:00:00+09:00',
                    'closesAt': '2026-10-03T00:00:00+09:00', 'canEnter': True,
                    'reason': None, 'nextOpensAt': None}
    closed = dict(availability, serverTime='2026-10-02T00:00:00+09:00', canEnter=False,
                  reason='OUTSIDE_SERVICE_HOURS', nextOpensAt=availability['opensAt'])
    waiting = dict(availability, canEnter=False, reason='CONVERSATION_CLOSING')
    home = {'child': {'childId': conversation['childId'], 'name': '도담', 'nickname': '도담이', 'characterId': 'dodam'},
            'greeting': '안녕', 'joinedAt': '2026-10-01T09:00:00+09:00', 'daysTogether': 2,
            'activeConversationId': None, 'menus': [{'code': 'TALK', 'status': 'AVAILABLE'},
            {'code': 'PLAY', 'status': 'COMING_SOON'}], 'availability': availability}
    for state in [availability, closed, waiting,
                  dict(closed, serverTime='2026-10-02T07:59:59+09:00'),
                  dict(availability, serverTime='2026-10-02T23:59:59+09:00')]:
        expect('ConversationAvailability', state)
        expect_semantic(state)
        fixture = deepcopy(home)
        fixture['availability'] = state
        fixture['menus'][0]['status'] = 'AVAILABLE' if state['canEnter'] else 'UNAVAILABLE'
        expect('ChildHome', fixture)
        expect_semantic(fixture)
    for state in [dict(availability, reason='OUTSIDE_SERVICE_HOURS'),
                  dict(availability, nextOpensAt=availability['opensAt']),
                  dict(closed, nextOpensAt=None), dict(waiting, nextOpensAt=availability['opensAt'])]:
        expect('ConversationAvailability', state, False)
    for state in [dict(availability, serviceDate='2026-10-01'),
                  dict(availability, opensAt='2026-10-02T07:00:00+09:00'),
                  dict(availability, closesAt='2026-10-02T23:59:59+09:00'),
                  dict(availability, serverTime='2026-10-02T07:59:59+09:00'),
                  dict(closed, serverTime='2026-10-02T08:00:00+09:00'),
                  dict(closed, nextOpensAt='2026-10-03T08:00:00+09:00'),
                  dict(waiting, serverTime='2026-10-02T00:00:00+09:00')]:
        expect_semantic(state, False)
    closing_home = deepcopy(home)
    closing_home['availability'] = waiting
    closing_home['menus'][0]['status'] = 'UNAVAILABLE'
    expect_semantic(dict(closing_home, activeConversationId=conversation['conversationId']), False)
    expect('ChildHome', dict(closing_home, closingConversationId=conversation['conversationId']), False)
    for menus in [[{'code': 'TALK', 'status': 'UNAVAILABLE'}, home['menus'][1]],
                  [home['menus'][0], home['menus'][0]], list(reversed(home['menus']))]:
        expect_semantic(dict(home, menus=menus), False)
    expect_semantic(dict(home, availability=closed, activeConversationId=conversation['conversationId'],
                         menus=[{'code': 'TALK', 'status': 'UNAVAILABLE'}, home['menus'][1]]), False)

    for value in [dict(page, turns=[dict(turn, sequence=2), turn]),
                  dict(page, hasNext=True, nextAfterSequence=99),
                  {'turns': [processing], 'processingTurnId': None},
                  dict(page, turns=[turn, dict(turn, sequence=2)]),
                  dict(turn, completedAt='2026-10-02T12:59:59+09:00'),
                  dict(ended, endedAt='2026-10-02T12:59:59+09:00'),
                  {'turnId': turn['turnId'], 'statusUrl': '/turns/66666666-6666-4666-8666-666666666666'},
                  {'turn': turn, 'audio': {'url': '/turns/66666666-6666-4666-8666-666666666666/audio'}}]:
        expect_semantic(value, False)

    lifecycle_cases()

    if api_only:
        print(json.dumps({'result': 'PASS', 'paths': len(paths), 'operations': len(operations),
                          'schemas': len(SCHEMAS), **COUNTS,
                          'source_schema_changes': SOURCE_SCHEMA_CHANGES,
                          'historical_schema_comparison': 'Only documented strict-end and MIME CRLF pattern corrections normalized before comparison',
                          'scope': 'OpenAPI and semantic fixtures only; Markdown, DB catalogs, diagrams and server integration not checked'}, ensure_ascii=False, indent=2))
        return

    api_files = [ROOT / 'Integrated_API_Spec.md', *sorted((ROOT / 'api').rglob('*.md'))]
    assert len(api_files) == 18, 'Expected API index, 10 guides and 7 schema catalogs'
    api_text = '\n'.join(path.read_text() for path in api_files)
    erd_text = (ROOT / 'Integrated_ERD_Design.md').read_text()
    tables = db_tables_from_markdown(erd_text)
    seen_schemas, seen_tables = set(), set()
    db_examples, dto_examples = {}, {}
    for content in [api_text, erd_text]:
        for block in re.finditer(r'```json\n(.*?)\n```', content, re.S):
            fixture = json.loads(block[1])
            marker = re.search(r'<!-- (json-schema|json-example|json-db-example|json-db-schema): ([A-Za-z_0-9]+) -->\s*$', content[:block.start()])
            kind, name = (marker[1], marker[2]) if marker else ('json-example', example_schema(fixture))
            if kind == 'json-schema':
                assert fixture == SCHEMAS[name], name
                assert name not in seen_schemas
                seen_schemas.add(name)
                COUNTS['markdown_schema_definitions'] += 1
            elif kind == 'json-db-schema':
                assert fixture == tables[name], name
                assert name not in seen_tables
                seen_tables.add(name)
                COUNTS['db_schema_definitions'] += 1
            elif kind == 'json-db-example':
                check_db_projection(name, fixture, tables)
                db_examples[name] = fixture
                COUNTS['db_json_examples'] += 1
            else:
                assert not accepts({'$ref': '#/components/schemas/' + name}, fixture), name
                semantic(fixture)
                if content is erd_text:
                    dto_examples[name] = fixture
                COUNTS['markdown_json_examples'] += 1
    assert seen_schemas == SCHEMAS.keys()
    assert seen_tables == tables.keys()
    for table, dto, id_field in [('children', 'ChildView', 'childId'),
                                 ('conversations', 'ConversationView', 'conversationId'),
                                 ('conversation_turns', 'TurnView', 'turnId')]:
        row = db_examples[table]
        mapped = {}
        for column, item in row.items():
            words = column.split('_')
            key = id_field if column == 'id' else words[0] + ''.join(word.title() for word in words[1:])
            if key in dto_examples[dto]:
                mapped[key] = item
        assert mapped.keys() == dto_examples[dto].keys(), dto
        for key, item in mapped.items():
            actual = dto_examples[dto][key]
            if key.endswith('At') and item is not None:
                assert datetime.fromisoformat(item) == datetime.fromisoformat(actual), (dto, key)
            else:
                assert item == actual, (dto, key)
        COUNTS['db_dto_mappings'] += 1
    manifest = json.loads((ROOT / 'support/diagrams/manifest.json').read_text())
    diagrams = {entry['id']: entry for entry in manifest['diagrams']}
    assert len(diagrams) == manifest['count'] and len(diagrams) >= 19
    for entry in manifest['diagrams']:
        source_text = (ROOT / entry['file']).read_text()
        block = re.search(r'<!-- diagram: ' + re.escape(entry['id']) + r' -->\s*```mermaid\n(.*?)\n```', source_text, re.S)
        assert block, entry['id']
        assert entry['sha256'] == hashlib.sha256(block[1].encode()).hexdigest(), entry['id']
        ET.parse(ROOT / ('support/diagrams/' + entry['id'] + '.svg'))
        COUNTS['rendered_diagrams'] += 1
    for filename, count in [('api/09-diagrams.md', 13), ('Integrated_ERD_Design.md', 6)]:
        content = (ROOT / filename).read_text()
        blocks = list(re.finditer(r'<!-- diagram: ([\w-]+) -->\s*```mermaid\n(.*?)\n```', content, re.S))
        assert len(blocks) == count
        for block in blocks:
            entry = diagrams[block[1]]
            assert entry['file'] == filename
            assert entry['sha256'] == hashlib.sha256(block[2].encode()).hexdigest()
            ET.parse(ROOT / ('support/diagrams/' + block[1] + '.svg'))
    for _, _, op in operations:
        assert op['operationId'] in api_text
    document_files = api_files + [ROOT / filename for filename in [
        'README.md', 'Integrated_ERD_Design.md', 'Decision_Record.md',
        'requirements/P0_Common_Spec.md', 'requirements/P0_Part1.md', 'requirements/P0_Part2.md']]
    for path in document_files:
        content = path.read_text()
        assert len(re.findall(r'^```', content, re.M)) % 2 == 0, path
        for match in re.finditer(r'\[[^\]]*\]\(([^\n)]+)\)', content):
            target = match.group(1).strip('<>')
            if target.startswith(('http:', 'https:', 'mailto:')):
                continue
            file, _, anchor = unquote(target).partition('#')
            assert not Path(file).is_absolute(), (path, target)
            linked = path.parent / file if file else path
            assert linked.exists(), (path, target)
            if anchor and linked.suffix == '.md':
                linked_text = re.sub(r'```.*?```', '', linked.read_text(), flags=re.S)
                ids = re.findall(r'<a id="([^"]+)"', linked_text)
                for heading in re.findall(r'^#+ (.+)$', linked_text, re.M):
                    ids.append(re.sub(r'[^\w\- ]', '', heading).lower().replace(' ', '-'))
                assert anchor in ids, (path, target, 'missing anchor')
            COUNTS['local_links'] += 1
    print(json.dumps({'result': 'PASS', 'paths': len(paths), 'operations': len(operations),
                      'schemas': len(SCHEMAS), **COUNTS,
                      'source_schema_changes': SOURCE_SCHEMA_CHANGES,
                          'historical_schema_comparison': 'Only documented strict-end and MIME CRLF pattern corrections normalized before comparison',
                      'scope': 'OpenAPI structure, source operation preservation, schema examples, negative fixtures, local document links; not server or AI integration'}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api-only', action='store_true', help='Skip generated Markdown, DB catalogs and diagrams')
    main(api_only=parser.parse_args().api_only)
