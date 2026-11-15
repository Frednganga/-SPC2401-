#!/usr/bin/env bash
# 7.3 failure tests. F1: subscribed client goes offline. F2: server down, client retries then gives up.
# For network delay run in a separate step (Linux, needs root): tc qdisc add dev lo root netem delay 200ms ; tc qdisc del dev lo root
cd "$(dirname "$0")/.."
./scripts/build.sh >/dev/null
mkdir -p evidence
J="java -cp out"
$J kyu.grade.RegistryMain >/dev/null 2>&1 & R=$!; sleep 1
$J -Dlog.file=evidence/f1-server.log -Drmi.timeout.ms=1500 kyu.grade.GradeServer >/dev/null 2>&1 & S=$!; sleep 1
$J kyu.grade.StudentClient watch DEAD1 >/dev/null 2>&1 & D=$!
$J kyu.grade.StudentClient watch LIVE1 > evidence/f1-live.log 2>&1 & L=$!
sleep 1
$J kyu.grade.LecturerClient submit LIVE1 SPC2401 80 >/dev/null
kill -9 $D; sleep 0.5                                   # DEAD1 crashes without unsubscribing
$J kyu.grade.LecturerClient publish SPC2401
sleep 4
echo "== F1: server log (dead listener skipped and removed, live one notified) =="
grep -E "notif|unreachable|publishMarks" evidence/f1-server.log
cat evidence/f1-live.log
echo "== F2: stop the server, then a lecturer call =="
kill $S; sleep 1
$J kyu.grade.LecturerClient submit LIVE1 SPC2401 90 2>&1 | tee evidence/f2-client.log | grep -E "WARN|ERROR|OK" | cut -c1-140
kill $L $R 2>/dev/null
