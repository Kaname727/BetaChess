#!/usr/bin/env python3
"""
Convert the lichess chess-openings TSVs (a.tsv … e.tsv) into a single
position-keyed TSV for BetaChess.

Input  : a.tsv, b.tsv, c.tsv, d.tsv, e.tsv   (from lichess-org/chess-openings)
Output : openings.tsv  with columns  eco <tab> name <tab> FEN:<epd>
"""

import csv
import io
import os
import sys

try:
    import chess
    import chess.pgn
except ImportError:
    sys.exit("python-chess is not installed. Run:  python3 -m pip install chess")

OUTPUT = "openings.tsv"
VOLUMES = ["a", "b", "c", "d", "e"]


def epd_after(pgn_text: str) -> str | None:
    """Replay the mainline of a PGN snippet and return the EPD of the final position."""
    game = chess.pgn.read_game(io.StringIO(pgn_text))
    if game is None:
        return None
    board = game.board()
    for move in game.mainline_moves():
        board.push(move)
    return board.epd()


def main() -> None:
    written = 0
    skipped = 0

    with open(OUTPUT, "w", encoding="utf-8", newline="") as out:
        out.write("# eco\tname\tFEN\n")
        for volume in VOLUMES:
            path = f"{volume}.tsv"
            if not os.path.isfile(path):
                print(f"[warn] {path} not found – skipping")
                continue
            with open(path, encoding="utf-8") as fh:
                reader = csv.DictReader(fh, delimiter="\t")
                for row in reader:
                    eco  = row.get("eco", "").strip()
                    name = row.get("name", "").strip()
                    pgn  = row.get("pgn", "").strip()
                    if not (eco and name and pgn):
                        skipped += 1
                        continue
                    epd = epd_after(pgn)
                    if not epd:
                        skipped += 1
                        continue
                    out.write(f"{eco}\t{name}\tFEN:{epd}\n")
                    written += 1

    print(f"[done] wrote {written} entries to {OUTPUT}"
          + (f" ({skipped} skipped)" if skipped else ""))


if __name__ == "__main__":
    main()