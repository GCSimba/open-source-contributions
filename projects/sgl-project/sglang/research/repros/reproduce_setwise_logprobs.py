"""Reproduce a scoring post-processing defect without a model or SGLang install.

Loads unchanged class definitions from a pinned public upstream revision.
This is a CPU-only component reproduction, not an HTTP or model integration test.
"""

import ast
import json
import math
import sys
import types
import urllib.request
from dataclasses import dataclass

REVISION = "fb9846d3e2d66811b372b28c73229e9dbbf54288"
SOURCE_PATH = "python/sglang/srt/managers/tokenizer_manager_score_mixin.py"
SOURCE_URL = f"https://raw.githubusercontent.com/sgl-project/sglang/{REVISION}/{SOURCE_PATH}"


def load_component():
    with urllib.request.urlopen(SOURCE_URL, timeout=30) as response:
        source = response.read().decode("utf-8")
    tree = ast.parse(source)
    definitions = [
        node
        for node in tree.body
        if isinstance(node, ast.ClassDef)
        and node.name in {"ScoreResult", "TokenizerManagerScoreMixin"}
    ]
    assert len(definitions) == 2
    module = types.ModuleType("_sglang_scoring_reproduction")
    module.__dict__.update(dataclass=dataclass, math=math)
    sys.modules[module.__name__] = module
    subset = ast.Module(
        body=[
            ast.ImportFrom(
                module="__future__",
                names=[ast.alias(name="annotations")],
                level=0,
            ),
            *definitions,
        ],
        type_ignores=[],
    )
    exec(compile(ast.fix_missing_locations(subset), SOURCE_URL, "exec"), module.__dict__)
    return module.TokenizerManagerScoreMixin, tree


def main():
    mixin, tree = load_component()
    manager = mixin()
    manager.is_generation = True
    labels = [[1, 2], [1, 2]]
    rows = [
        [(-1.0, 1, None), (-2.0, 2, None)],
        [(-3.0, 1, None), (-4.0, 2, None)],
        [(-5.0, 1, None), (-6.0, 2, None)],
    ]

    pointwise = manager._process_single_item_scoring_results(
        [
            {"meta_info": {"output_token_ids_logprobs": [rows[0]]}},
            {"meta_info": {"output_token_ids_logprobs": [rows[1]]}},
        ],
        labels,
        apply_softmax=False,
        return_token_logprobs=True,
    )
    assert pointwise.token_logprobs == [[-1.0, -2.0], [-3.0, -4.0]]

    batched_setwise = manager._process_single_item_scoring_results(
        [
            {"meta_info": {"input_token_ids_logprobs": [rows[0]]}},
            {"meta_info": {"input_token_ids_logprobs": rows[1:]}},
        ],
        labels,
        apply_softmax=False,
        per_item_matrix=True,
        return_token_logprobs=True,
    )
    assert [len(item) for item in batched_setwise.scores] == [1, 2]
    assert batched_setwise.token_logprobs == []

    fused_setwise = manager._process_multi_item_extraction_results(
        {"meta_info": {"input_token_ids_logprobs": rows}},
        per_item_anchor_counts=[1, 2],
        apply_softmax=False,
        label_token_ids=labels,
    )
    assert fused_setwise.scores == batched_setwise.scores
    assert fused_setwise.token_logprobs is None

    cls = next(
        n for n in tree.body
        if isinstance(n, ast.ClassDef) and n.name == "TokenizerManagerScoreMixin"
    )
    score_request = next(n for n in cls.body if n.name == "score_request")
    fused_calls = [
        n for n in ast.walk(score_request)
        if isinstance(n, ast.Call)
        and isinstance(n.func, ast.Attribute)
        and n.func.attr == "_process_multi_item_extraction_results"
    ]
    assert len(fused_calls) == 1
    assert not any(k.arg == "return_token_logprobs" for k in fused_calls[0].keywords)

    print(json.dumps({
        "revision": REVISION,
        "pointwise_token_logprobs": pointwise.token_logprobs,
        "batched_setwise_token_logprobs": batched_setwise.token_logprobs,
        "fused_setwise_token_logprobs": fused_setwise.token_logprobs,
        "setwise_score_row_counts": [len(item) for item in batched_setwise.scores],
        "raw_logprob_rows_available": len(rows),
        "fused_caller_forwards_return_token_logprobs": False,
        "status": "Defect reproduced in unchanged upstream post-processing",
        "validation_scope": "CPU component only; no real model, HTTP server, or full repository tests",
    }, indent=2))


if __name__ == "__main__":
    main()
