
#include <stdio.h>
#include "rocc.h"

int main(){

    int xd = 0;
    int xs1 = 0;
    int xs2 = 0;
    int rd = 0;
    int rs1 = 4;
    int rs2 = 3;
    int funct = 3;
    int a[4];
    int s, l;

    printf("Start simulation\n");

    // conventions: 
        // load : funct === 0
        // store : funct === 1
        // get_load : funct === 2
        // exchange : funct === 3s

    rs1 = 3;
    rs2 = 5;
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 1);
    printf("First operation -> store completed: result is  %d  (expected is 13)\n", rd); // mem(5) := 3
    

    // do a load of the memory address 5 in all the PEs
    // rd = mem(5)
    
    rs2 = 5;
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 0); // queue := mem(5) [== 3]
    printf("Second operation -> load completed: result is  %d  (expected is 13)\n", rd );
    

    // get the load result of the last load operation
    int i = 0;
    for(i =0; i < 4; i++) {
        rd = i;
        ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 2);
        printf(" %d -th operation operation -> get_load completed: result is  %d  (expected is 3)\n", i + 3, rd); // rd := queue
    }

    // data exchange test: exchange(src = 6, n = 3, dest = 9)

    // store another set of values

    printf("Now data exchange!\n");

    int n = 3;
    rs2 = 6;
    rs1 = 20;

    for(i = 0; i < n; i++){
        rd = i;
        rs1 = 20 + i;
        rs2 = 6 + i;
        ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 1);
        printf(" %d -th prelinimary store done, result is : %d (expected 13)\n", i+1, rd); // mem(6 + i) := 10 + i
                                                         // mem(6) := 10
                                                         // mem(7) := 11
                                                         // mem(8) := 12
                                                         
    }

    // data exchange command

    rd = 5;
    rs1 = 6 + (n << 16); // source + n
    rs2 = 9; // dest
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 3); //exchange(src = 6, n = 3, dest = 9)
    printf("Exchange done, result is : %d (expected 13)\n", rd);

    // now load to see if results are actually stored

    rd = 5;
    rs2 = 9 + (1 << 16); // first memory (up)
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 0); // queue := memUP(9)
    printf("Load done, result is : %d (expected 13)\n", rd);

    for(i = 0; i < 4; i++){
        ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 2); // queue := memUP(9)
        printf("Load done, result is : %d (expected 20)\n", rd);
    }

    rd = 5;
    rs2 = 10 + (1 << 16); // first memory (up)
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 0); // queue := memUP(9)
    printf("Load done, result is : %d (expected 13)\n", rd);

    for(i = 0; i < 4; i++){
        ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 2); // queue := memUP(9)
        printf("Load done, result is : %d (expected 21)\n", rd);
    }

    rd = 5;
    rs2 = 11 + (1 << 16); // first memory (up)
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 0); // queue := memUP(9)
    printf("Load done, result is : %d (expected 13)\n", rd);

    for(i = 0; i < 4; i++){
        ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 2); // queue := memUP(9)
        printf("Load done, result is : %d (expected 22)\n", rd);
    }






    printf("End simulation\n");

    return 0;
}