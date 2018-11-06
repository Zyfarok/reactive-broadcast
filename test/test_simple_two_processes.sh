#!/bin/bash
time_to_finish=2

init_time=5


#start 2 processes, each fifo broadcasting 1 messages

echo "log file should look like this " > expected

#Start the 2 processes
for i in `seq 1 2`
do
    java -jar DaProject-all-1.0-SNAPSHOT.jar $i test/membership_test_simple.txt &
    da_proc_id[$i]=$!
    echo "process ${da_proc_id[$i]} started"
done

echo "processes started"

sleep $init_time

#Pause processes 1
for i in `seq 1 1`
do
	kill -STOP ${da_proc_id[$i]} #pause process
    echo "Process $i paused"
done

sleep 2

#Send from 2
for i in `seq 2 2`
do
	kill -USR2 ${da_proc_id[$i]}
    echo "msg broadcast for process $i"
done

# Deliver not expected yet since 3 is paused and 4 can only send to 5.

sleep 5

#Resume process 1
kill -CONT ${da_proc_id[1]} #resume process
echo "Process 1 resumed"

sleep 10

#Kill all processes
for i in `seq 1 2`
do
	kill -TERM ${da_proc_id[${i}]}
    echo "process ${i} ended"
done

echo "processes closed"
exit 0
