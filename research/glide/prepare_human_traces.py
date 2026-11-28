"""Prepare disjoint English calibration and evaluation traces from How We Swipe.

This writes derived files for local evaluation. Do not redistribute the logs or
derived trajectories. Source data: https://osf.io/sj67f/
"""
import argparse
import random
import re
import statistics
import zipfile
from collections import defaultdict
from pathlib import Path


def traces(lines):
    points = []
    for line in lines:
        columns = line.split()
        if len(columns) != 12:
            points = []
            continue
        _, stamp, width, height, event, x, y, _, _, _, word, error = columns
        if event == "touchstart":
            points = []
            initial = int(stamp)
        if event != "touchstart" and not points:
            continue
        if event not in {"touchstart", "touchmove", "touchend"}:
            points = []
            continue
        width, height, x, y = map(float, (width, height, x, y))
        if width <= 0 or not (0 <= x <= width and 0 <= y <= height):
            points = []
            continue
        points.append((10 * x / width, 10 * y / width))
        if event == "touchend":
            if (error == "0" and re.fullmatch(r"[a-z]{2,32}", word)
                    and 2 <= len(points) <= 512 and 65 <= int(stamp) - initial <= 1_200):
                yield word, points
            points = []


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    calibrate = defaultdict(list)
    rng = random.Random(8712)
    examples = []
    with zipfile.ZipFile(args.archive) as archive:
        members = sorted(name for name in archive.namelist() if name.endswith(".log"))
        rng.shuffle(members)
        for index, name in enumerate(members):
            items = list(traces(archive.read(name).decode("utf-8").splitlines()[1:]))
            if index < 100:
                for word, points in items:
                    calibrate[word[0]].append(points[0])
                    calibrate[word[-1]].append(points[-1])
            elif items:
                examples.append(rng.choice(items))
                if len(examples) == 250:
                    break
    if len(examples) != 250 or set(calibrate) != set("abcdefghijklmnopqrstuvwxyz"):
        raise ValueError("Insufficient disjoint test traces or calibration coverage")
    args.output.mkdir(parents=True, exist_ok=True)
    with (args.output / "centers.tsv").open("w", encoding="utf-8") as output:
        for key, values in sorted(calibrate.items()):
            x, y = map(statistics.median, zip(*values))
            print(f"{key}\t{x:.6f}\t{y:.6f}", file=output)
    with (args.output / "traces.tsv").open("w", encoding="utf-8") as output:
        for word, points in examples:
            encoded = " ".join(f"{x:.6f},{y:.6f}" for x, y in points)
            print(f"{word}\t{encoded}", file=output)


if __name__ == "__main__":
    main()
