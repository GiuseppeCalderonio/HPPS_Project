package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class PushNewValuesTest(m : KeepReg) extends PeekPokeTester(m){

    val n = 3

    for(i <- 0 until n){
        poke(m.io.keep, false.B )
        poke(m.io.in, i)

        step(1)

        expect(m.io.out, i)
    }


}

class KeepValuesTest(m : KeepReg) extends PeekPokeTester(m){

    val n = 3

    expect(m.io.out, 1.U)

    poke(m.io.keep, false.B )
    poke(m.io.in, 20.U)

    step(1)

    expect(m.io.out, 20.U)

    for(i <- 0 until n){
        poke(m.io.keep, true.B )
        poke(m.io.in, i)

        step(1)

        expect(m.io.out, 20.U)
    }

    poke(m.io.keep, false.B )
    poke(m.io.in, 10.U)

    step(1)

    expect(m.io.out, 10.U)

}

class KeepRegTest extends ChiselFlatSpec {



  val testerArgs = Array("")

  behavior of "PushNewValuesTest"
  it should "return always new values when !keep" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new KeepReg(32)) {
      c => new PushNewValuesTest(c)
    } should be (true)
  }

  behavior of "KeepValuesTest"
  it should "keep values in the register when specified" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new KeepReg(32)) {
      c => new PushNewValuesTest(c)
    } should be (true)
  }

  
  //val testerArgs = Array("")

  val n = 3

  behavior of "PushNewValuesTest"
  it should "work as a normal register when keep = false" in {
    chisel3.iotesters.Driver.execute( Array(""), () => new KeepReg(n)) {
      c => new PushNewValuesTest(c)
    } should be (true)
  } 

  behavior of "KeepValuesTest"
  it should "keep inside the last result until keep = true" in {
    chisel3.iotesters.Driver.execute( Array(""), () => new KeepReg(n)) {
      c => new PushNewValuesTest(c)
    } should be (true)
  } 
  
}