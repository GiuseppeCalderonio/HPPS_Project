
#include <stdio.h>
#include "rocc.h"
#define LOAD 0
#define STORE 1
#define GET_LOAD 2
#define EXCHANGE 3
#define MAIN 0
#define UP 1
#define DOWN 2
#define LEFT 3
#define RIGHT 4
#define PES 4

int main(){

    int xd = 0;
    int xs1 = 0;
    int xs2 = 0;
    int rd = 0;
    int rs1 = 4;
    int rs2 = 3;
    int funct = 3;

    printf("Start simulation\n");

    // conventions: 
        // load : funct === 0
        // store : funct === 1
        // get_load : funct === 2
        // exchange : funct === 3


    store(5, 3, MAIN);  // memMain(5) := 3
    

    load(5, MAIN, 3); // rd := memMain(5) [== 3]
    

    printf("Now data exchange!\n");

    int n = 10;
    rs2 = 6;
    rs1 = 20;

    int i = 0;

    for(i = 0; i < n; i++){
        store(6 + i, 20 + i, MAIN); // memMain(6 + i) := 20 + i
        // memMain(6) := 20
        // memMain(7) := 21
        // memMain(8) := 22
        // memMain(9) := 23
        // memMain(10) := 24
        // memMain(11) := 25
        // memMain(12) := 26
        // memMain(13) := 27
        // memMain(14) := 28
        // memMain(15) := 29

                             
    }

    // data exchange command

    exchange(6, 10, 9);

    // now load to see if results are actually stored

    for(i = 0; i < n; i++){
        load(9 + i, UP, 20 + i);
        load(9 + i, DOWN, 20 + i);
        load(9 + i, LEFT, 20 + i);
        load(9 + i, RIGHT, 20 + i);
    }



    printf("End simulation\n");

    return 0;
}

void load(int address, int memory, int expected){

    int rs2 = address + (memory << 16);
    int rs1 = 0;
    int rd = 0;
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, LOAD);
    printf("Load completed, result is : %d (expected 13)\n", rd);

    for(int i = 0; i < PES; i++){
        ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, GET_LOAD);
        printf("Get_Load completed, result is : %d (expected %d)\n", rd, expected);
        if (rd != expected){
            printf("ERROR! Watch above !\n");
            exit(0);
        }
    }
}

void store(int address, int value, int memory){
    int rs1 = value;
    int rs2 = address + (memory << 16);
    int rd = 0;
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, STORE);
    printf("Store completed: result is  %d  (expected is 13)\n", rd);
}

void exchange(int src, int n_size, int dest){

    int rs1 = src + (n_size << 16);
    int rs2 = dest;
    int rd = 0;
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, EXCHANGE);
    printf("Exchange completed: result is  %d  (expected is 13)\n", rd);

}