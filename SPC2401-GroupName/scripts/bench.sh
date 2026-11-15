#!/usr/bin/env bash
# 7.4 performance. Simulates 4 ms CPU + 6 ms disk per getMark (the numbers from 4.3).
# Output: evidence/bench.csv
set -e
cd "$(dirname "$0")/.."
./scripts/build.sh
mkdir -p evidence
OUT=evidence/bench.csv; echo "serverWorkers,mode,clientThreads,calls,meanLatencyMs,throughputPerSec" > $OUT
java -cp out kyu.grade.RegistryMain > /dev/null 2>&1 & R=$!
sleep 1
for W in 1 2 4 8 16; do
  java -cp out -Dserver.workers=$W -Dsim.cpu.ms=4 -Dsim.disk.ms=6 -Dlog.file=evidence/bench-server.log kyu.grade.GradeServer > /dev/null 2>&1 & S=$!
  sleep 1
  echo "$W,$(java -cp out kyu.grade.Bench remote 32 100)" >> $OUT   # 32 client threads keep the server saturated
  kill $S; sleep 1
done
# local vs remote call, no simulated work
echo "-,$(java -cp out kyu.grade.Bench local 1 5000 | tail -1)" >> $OUT
java -cp out kyu.grade.GradeServer > /dev/null 2>&1 & S=$!; sleep 1
echo "-,$(java -cp out kyu.grade.Bench remote 1 5000)" >> $OUT
kill $S $R 2>/dev/null || true
cat $OUT
