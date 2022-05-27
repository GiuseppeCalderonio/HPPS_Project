package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class ComputeDestAddressTest(m : DestAddressCalculator) extends PeekPokeTester(m){

    var n = 5

    val offset = 20

    for(i <- 0 until n){
        poke(m.io.in_dest_addr, (offset + 5*i).U )
        poke(m.io.in_dest_offset, i.U )
        poke(m.io.count_done, false.B )

        step(1)

        expect(m.io.out_dest_addr, (offset + i).U)
    }

    poke(m.io.in_dest_addr, 3.U )
    poke(m.io.in_dest_offset, 5.U )
    poke(m.io.count_done, true.B )

    step(1)

    expect(m.io.out_dest_addr, (3).U )

    poke(m.io.in_dest_addr, 30.U )
    poke(m.io.in_dest_offset, 2.U )
    poke(m.io.count_done, false.B )

    step(1)

    expect(m.io.out_dest_addr, 32.U )
}



class DestAddressCalculatorTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  
  behavior of "ComputeDestAddressTest"
  it should "Compute the address + offset correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new DestAddressCalculator()) {
      c => new ComputeDestAddressTest(c)
    } should be (true)
  }
  
}