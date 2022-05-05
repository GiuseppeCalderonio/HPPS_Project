
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

    printf("Start simulation\n");

    // conventions: 
        // load : funct === 0
        // store : funct === 1
        // get_load : funct === 2


    // do a store of 3 in all the PEs in the address 5
    rs1 = 3;
    rs2 = 5;
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 1); // 2952794129 = 0xb0001011
    printf("First operation -> store completed: result is  %d  (expected is 0)\n", rd);

    // do a load of the memory address 5 in all the PEs
    rs1 = 5;
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 0);
    printf("Second operation -> load completed: result is  %d  (expected is 0)\n", rd);

    // execute a nop
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 2);

    // get the load result of the last load operation
    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 2);
    printf("Third operation operation -> get_load completed: result is  %d  (expected is 3)\n", rd);

    // load rs1, 4
    // load rs2, 3
    // opcode ...
    // store resp.rd, resp.data


    printf("End simulation\n");

    return 0;
}