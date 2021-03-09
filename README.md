# Reactive Broadcast
FIFO and Causal Broadcast implement in Java with ReactiveX 

## Authors:
    Clément Burgelin
    Vikalp Kamdar
    Pierre Thévenet

## Build & Run
    make
    ./da_proc $id $membership_file_path $n

Which is equivalent to :

    /gradlew makeJar
    cp build/libs/*.jar .
    java -jar DaProject-all-*.jar $id $membership_file_path $n

## Clean
    make clean

Which is equivalent to :

    rm *.jar
    ./gradlew clean
