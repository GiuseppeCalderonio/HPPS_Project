
#include <stdio.h>
#include "rocc.h"

int main(){

    int X = 1;
    int xd = 0;
    int xs1 = 0;
    int xs2 = 0;
    int rd = 0;
    int rs1 = 4;
    int rs2 = 3;
    int funct = 3;

    printf("Start simulation\n");


    ROCC_INSTRUCTION_DSS(0, rd, rs1, rs2, 0);


    printf("End simulation, result is %d = %d + %d \n", rd, rs1, rs2);

    return 0;
}