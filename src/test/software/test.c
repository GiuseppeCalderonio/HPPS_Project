#include <stdio.h>
#include <rocc.h>

int main(){

    int a;

    printf("Start simulation");

    a = ROCC_INSTRUCTION_DSS(0, 2, 4, 3, 0);

    printf("End simulation, result is %d", a);

    return a;
}