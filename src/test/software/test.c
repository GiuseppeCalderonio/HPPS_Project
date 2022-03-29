
#include <stdio.h>
#include "rocc.h"

int main(){

    int a = 0;

    printf("Start simulation\n");

    ROCC_INSTRUCTION_DSS(0, a, 3, 3, 0);

    printf("End simulation, result is %d \n", a);

    return 0;
}

/*
#include <stdio.h>

int main(void)
{
    printf("Hello, World!\n");
    return 0;
}
*/