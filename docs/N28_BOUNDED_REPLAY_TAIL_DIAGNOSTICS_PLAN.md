# N28 — Bound oversized replay frame-count diagnostics

Date: 2026-10-26  
Status: planned; exact-SHA source-only CI pending.

## Engineering question

Can the replay comparator report a short-vs-long trace mismatch without retaining an oversized untrusted tail line in `Divergence`?

N27 bounded lines before parsing while both iterators had a frame. However, when one trace ended first, the final `frame-count` branch returned `left.next()` or `right.next()` directly. That path bypassed the line-length guard and could retain an arbitrarily large tail string in the result.

Source inspection identifies the path, but it cannot prove the returned diagnostic never contains the payload or that bounded diagnostics preserve existing behavior. Byte/length-focused JUnit regressions are required.

## Planned implementation

- Add a single helper for the extra frame returned by a `frame-count` mismatch.
- Return an in-bound tail line verbatim for compatibility.
- For a line longer than `MAX_TRACE_LINE_LENGTH`, return only a compact marker containing observed length and configured limit; never echo the content.
- Do not change frame consumption, parsing, schema rules, or the 100,000-frame cap.

## Acceptance criteria

1. An oversized expected-only tail returns `frame-count`, the correct frame index, and a bounded marker—not the original line.
2. An oversized actual-only tail has the same protection.
3. A short tail remains verbatim in the diagnostic, preserving the existing contract.
4. Existing deterministic replay, malformed trace, schema, first-divergence, empty trace, and frame-cap tests remain green.
5. Run the actual free native workflow (`:core:test`) and Mainline JS/Python checks on the resulting source/test checkpoint; record exact SHA, run URLs and logs.
6. Do not assemble/package/upload an APK: this is a pure JVM diagnostic-retention question.

## Safety and scope

No new dependency, copied implementation, game art/audio, APK packaging, release, merge, auto-merge, or write to `main`. PR #2 remains separate from collision PR #1. Device playback, Xiaomi Pad 5 behavior, and Symbian/EKA2L1 parity remain separate acceptance gates.
