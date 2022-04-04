
#include <stdio.h>
#include "rocc.h"

int main(){

    int X = 0;
    int xd = 0;
    int xs1 = 0;
    int xs2 = 0;
    int rd = 0;
    int rs1 = 0;
    int rs2 = 0;
    int funct = 0;

    printf("Start simulation\n");


    ROCC_INSTRUCTION_DSS(X, rd, rs1, rs2, funct);


    printf("End simulation, result is %d = %d + %d \n", rd, rs1, rs2);

    return 0;
}