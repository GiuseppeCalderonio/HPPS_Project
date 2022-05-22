package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class SourceCalculatorTest(m : SourceAddressCalculator) extends PeekPokeTester(m){

    var i = 0

    
    poke(m.io.count_done, true.B )
    poke(m.io.offset, 3.U )
    poke(m.io.address, (4 *10).U )

    step(1)

    expect(m.io.rs1, ( 4 * 10).U )

    i = 1

    poke(m.io.count_done, false.B )
    poke(m.io.offset, i.U )
    poke(m.io.address, (i * 10).U )
    step(1)

    expect(m.io.rs1, (i + i * 10).U )

    for(i <- 2 until 10){

        poke(m.io.count_done, false.B )
        poke(m.io.offset, i.U )
        poke(m.io.address, (i*10).U )

        step(1)

        expect(m.io.rs1, (10 + i).U)
    }

    i = 10

    poke(m.io.count_done, true.B )
    poke(m.io.offset, i.U )
    poke(m.io.address, (i * 10).U )

    step(1)

    expect(m.io.rs1, ( i * 10).U )

    i = 11

    poke(m.io.count_done, false.B )
    poke(m.io.offset, i.U )
    poke(m.io.address, (i * 10).U )

    step(1)

    expect(m.io.rs1, (i + i * 10).U )

    i = 12

    poke(m.io.count_done, true.B )
    poke(m.io.offset, i.U )
    poke(m.io.address, (i * 10).U )

    step(1)

    expect(m.io.rs1, ( i * 10).U )


}

class SourceAddressCalculatorTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  /*
  behavior of "AddressCounterStateMachineTest"
  it should "Change state correctly when triggered" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new AddressCalculator_StateMachine()) {
      c => new AddressCounterStateMachineTest(c)
    } should be (true)
  }
  */

  behavior of "SourceCalculatorTest"
  it should "Compute the right address" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new SourceAddressCalculator()) {
      c => new SourceCalculatorTest(c)
    } should be (true)
  }


}