package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._

class CountingTest(m : Counter, n : Int) extends PeekPokeTester(m, n){

  // idle
  
  poke(m.io.reset_, false.B )
  poke(m.io.input, 20.U )

  step(1) // idle -> idle

  expect(m.io.done, true.B )
  
  poke(m.io.reset_, true.B )
  poke(m.io.input, n.U )
  poke(m.io.stall, false.B )
  expect(m.io.value, 0.U )

  step(1)

  expect(m.io.value, 0.U )
  expect(m.io.done, false.B )
  expect(m.io.output, n.U )



  for(i <- 1 until n){
    
    print(i)
    poke(m.io.input, (n+i).U )
    poke(m.io.stall, false.B )
    poke(m.io.reset_, false.B )

    step(1) // counting from 0 to 4

    expect(m.io.value, (i).U )
    expect(m.io.done, false.B )
    expect(m.io.output, n.U )

    
  }

  step(1)

  
  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, n.U )
  poke(m.io.stall, false.B )

  for(i <- 0 until n){
    print(i)
    step(1)

    expect(m.io.value, i.U )
    expect(m.io.done, false.B )

    poke(m.io.reset_, false.B )
  }

  step(1)

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, n.U )
  poke(m.io.stall, false.B )

  step(1)

  poke(m.io.reset_, false.B )

  step(n-1)

  expect(m.io.done, false.B )
  expect(m.io.value, 4.U )

  step(1)

  expect(m.io.done, true.B )

}

class StallTest(m: Counter, n: Int) extends PeekPokeTester(m, n){

  poke(m.io.reset_, true.B )
  poke(m.io.input, n.U )
  poke(m.io.stall, false.B )

  step(1)
  poke(m.io.reset_, false.B )
  expect(m.io.value, 0.U )
  step(1)

  expect(m.io.done, false.B )
  expect(m.io.value, 1.U )

  for(i <- 0 until 20){
    poke(m.io.stall, true.B )

    step(1)

    expect(m.io.done, false.B )
    expect(m.io.value, 1.U )
  }

  poke(m.io.stall, false.B )
  
  step(3)

  expect(m.io.done, false.B )
  expect(m.io.value, (n-1).U )

  poke(m.io.reset_, false.B )

  step(1)

  expect(m.io.done, true.B )


}


class CornerCasesTest(m : Counter, n : Int) extends PeekPokeTester(m, n){

  poke(m.io.reset_, true.B )
  poke(m.io.input, 0.U )
  poke(m.io.stall, true.B )

  step(1) // 0 -> 1

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, 0.U )
  poke(m.io.stall, false.B )

  step(1) // 1 -> 2

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, 1.U )
  poke(m.io.stall, false.B )

  step(1) // 2 -> 3 

  expect(m.io.done, false.B )
  expect(m.io.value, 0.U )

  poke(m.io.reset_, false.B )

  step(1) // 3 -> 4

  expect(m.io.done, true.B )
  expect(m.io.equal_out, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, 1.U )
  poke(m.io.stall, false.B )

  step(1) // 4 -> 5

  expect(m.io.done, false.B )
  expect(m.io.value, 0.U )

  poke(m.io.reset_, false.B )

  step(1) // 5 -> 6

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, 1.U )
  poke(m.io.stall, true.B )

  step(1) // 6 -> 7

  poke(m.io.reset_, false.B )

  step(3) // 7 -> 10

  expect(m.io.done, false.B )
  expect(m.io.value, 0.U )

  poke(m.io.stall, false.B )

  step(1) // 10 -> 11

  expect(m.io.done, true.B )
}

class CounterTest extends ChiselFlatSpec {

  val testerArgs = Array("")
  
  val n = 5
/*
  behavior of "CountingTest"
  it should "Count up to N correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new Counter()) {
      c => new CountingTest(c, n)
    } should be (true)
  }

  behavior of "StallTest"
  it should "Stall the count when stall = true" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new Counter()) {
      c => new StallTest(c, n)
    } should be (true)
  }

  behavior of "CornerCasesTest"
  it should "Stall the count when stall = true" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new Counter()) {
      c => new CornerCasesTest(c, 1)
    } should be (true)
  }
  */
  
}