#!/usr/bin/env bash
# 7.2 integration test: registry, server, 2 student clients, 1 lecturer client = 5 separate JVM processes.
set -e
cd "$(dirname "$0")/.."
./scripts/build.sh
mkdir -p evidence
J="java -cp out"
$J kyu.grade.RegistryMain > evidence/it-registry.log 2>&1 & R=$!
sleep 1
$J -Dlog.file=evidence/it-server.log kyu.grade.GradeServer > /dev/null 2>&1 & S=$!
sleep 1
$J kyu.grade.StudentClient watch KYU001 > evidence/it-student1.log 2>&1 & A=$!
$J kyu.grade.StudentClient watch KYU002 > evidence/it-student2.log 2>&1 & B=$!
sleep 1
$J kyu.grade.LecturerClient submit KYU001 SPC2401 78 | tee evidence/it-lecturer.log
$J kyu.grade.LecturerClient submit KYU002 SPC2401 65 | tee -a evidence/it-lecturer.log
$J kyu.grade.LecturerClient publish SPC2401          | tee -a evidence/it-lecturer.log
sleep 2
kill $A $B $S $R 2>/dev/null || true
if grep -q "mark=78" evidence/it-student1.log && grep -q "mark=65" evidence/it-student2.log; then
  echo "INTEGRATION PASS: both students received the notification"
else
  echo "INTEGRATION FAIL"; exit 1
fi
