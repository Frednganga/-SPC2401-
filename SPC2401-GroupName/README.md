# SPC2401 KYUCampusGrade (Java RMI)

Requires JDK 17+ (tested on 21). Run every command from the repository root.

## Build
    ./scripts/build.sh            # compiles src/ into out/

## Run (each in its own terminal)
    java -cp out kyu.grade.RegistryMain                        # 1. registry
    java -cp out kyu.grade.GradeServer                         # 2. server
    java -cp out kyu.grade.StudentClient watch KYU001          # 3. student 1 (stays running)
    java -cp out kyu.grade.StudentClient watch KYU002          # 4. student 2
    java -cp out kyu.grade.LecturerClient submit KYU001 SPC2401 78
    java -cp out kyu.grade.LecturerClient submit KYU002 SPC2401 65
    java -cp out kyu.grade.LecturerClient publish SPC2401      # students print the notification
    java -cp out kyu.grade.StudentClient get KYU001 SPC2401

## Configuration
Edit `config.properties` or pass `-Dkey=value` (e.g. `-Dregistry.host=10.0.0.5 -Dregistry.port=2099`).
The server must run on the same machine as the registry (RMI rule). Use `-Dserver.host=<ip>` and
`-Dclient.host=<ip>` when machines differ.

## Tests
    # Unit tests (Maven)
    mvn test
    # Integration: registry + server + 2 students + lecturer, separate processes
    ./scripts/integration.sh
    # Concurrency: 25 clients x 40 submits (server and registry must be running)
    java -cp out kyu.grade.LoadTest 25 40
    # Failure tests (offline client, server stopped)
    ./scripts/failure.sh
    # Performance (workers 1,2,4,8,16; local vs remote) -> evidence/bench.csv
    ./scripts/bench.sh

## Design notes
- `submitMark(requestId, ...)` is applied once per requestId (at-most-once); the client reuses the ID on retry.
- `publishMarks` copies the subscriber list under a short lock, then notifies from a thread pool: no lock is held during a callback.
- Subscriptions carry a lease (`lease.ms`); students renew at half the lease; expired or unreachable listeners are removed.
- `subscribe` is per student (not per unit), as in the scenario interface, so a student is told about every published unit.
