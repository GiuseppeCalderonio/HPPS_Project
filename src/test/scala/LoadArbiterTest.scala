package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class ArbiterCorrectTest(m : LoadArbiter, n : Int) extends PeekPokeTester(m){

  poke(m.io.in_valid, false.B )
  poke(m.io.out.ready, true.B )

  step(1) // 0 -> 1

  expect(m.io.in_ready, true.B )
  expect(m.io.out.valid, false.B )

  poke(m.io.in_valid, true.B )
  poke(m.io.out.ready, true.B )

  for(i <- 0 until n ){
    poke(m.io.in_vec(i), (20 + 2*i).U ) // 0=20, 1=22, 2=24, 3=26
  }

  step(1) // 1 -> 2

  expect(m.io.in_ready, false.B )
  expect(m.io.out.valid, true.B )
  expect(m.io.out.bits.data, 20.U )

  for(i <- 1 until n){

    poke(m.io.in_valid, false.B )
    poke(m.io.in_vec(i), 34.U )
    poke(m.io.out.ready, true.B )

    step(1)

    expect(m.io.in_ready, false.B )
    expect(m.io.out.valid, true.B )
    expect(m.io.out.bits.data, (20 + 2*i).U )


  }

  step(1)

  expect(m.io.in_ready, true.B )
  expect(m.io.out.valid, false.B )

  for(i <- 0 until n ){
    poke(m.io.in_vec(i), (30 + 2*i).U ) // 0=20, 1=22, 2=24, 3=26
  }

  poke(m.io.in_valid, true.B )

  step(1)

  expect(m.io.in_ready, false.B )
  expect(m.io.out.valid, true.B )
  expect(m.io.out.bits.data, 30.U )

  for(i <- 1 until n){

    poke(m.io.in_valid, false.B )
    poke(m.io.in_vec(i), 44.U )
    poke(m.io.out.ready, true.B )

    step(1)

    expect(m.io.in_ready, false.B )
    expect(m.io.out.valid, true.B )
    expect(m.io.out.bits.data, (30 + 2*i).U )


  }

  step(1)

  expect(m.io.in_ready, true.B )
  expect(m.io.out.valid, false.B )

}

class LoadArbiterTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  val n = 4
/*
  behavior of "ArbiterCorrectTest"
  it should "foreward all the values from all the inputs from the first to the last for n Cycles" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new LoadArbiter(16, n)) {
      c => new ArbiterCorrectTest(c, n)
    } should be (true)
  } */
}