package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class StateMachineTest(m : Counter_StateMachine) extends PeekPokeTester(m){

    // idle

    expect(m.io.done, true.B )
    expect(m.io.mux, false.B )

    poke(m.io.reset_, false.B )
    poke(m.io.is_input_zero, true.B )

    step(1) // idle -> idle

    expect(m.io.done, true.B )
    expect(m.io.mux, false.B )

    // idle

    poke(m.io.reset_, true.B )
    poke(m.io.is_input_zero, true.B )

    step(1) // idle -> idle

    expect(m.io.done, true.B )
    expect(m.io.mux, false.B )

    // idle

    poke(m.io.reset_, true.B )
    poke(m.io.is_input_zero, false.B )

    step(1) // idle -> counting_first

    expect(m.io.done, false.B )
    expect(m.io.mux, true.B )

    poke(m.io.reset_, true.B )
    poke(m.io.equal, false.B )

    step(1) // countig_first -> counting

    expect(m.io.done, false.B )
    expect(m.io.mux, false.B )

    for(i <- 0 until 3){
        // counting
        poke(m.io.equal, false.B )
        poke(m.io.reset_, true.B )

        step(1) // counting -> counting

        expect(m.io.done, false.B )


    }

    // counting

    poke(m.io.equal, true.B )

    step(1) // counting -> idle

    expect(m.io.done, true.B)

    // idle

    poke(m.io.reset_, true.B )
    poke(m.io.is_input_zero, false.B )

    step(1) // idle -> counting_first

    expect(m.io.done, false.B )
    expect(m.io.mux, true.B )

    // counting_first

    poke(m.io.equal, true.B )

    step(1) // counting_first -> indle

    expect(m.io.done, true.B )
    expect(m.io.mux, false.B )

    // idle

    
}

class CountingTest(m : Counter, n : Int) extends PeekPokeTester(m, n){

  // idle
  poke(m.io.reset_, false.B )
  poke(m.io.input, 20.U )

  step(1) // idle -> idle

  expect(m.io.done, true.B )

  // idle

  for(i <- 0 until n){
    
    poke(m.io.reset_, true.B )
    poke(m.io.input, (n+i).U )
    poke(m.io.stall, false.B )

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
  }

  step(1)

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, n.U )
  poke(m.io.stall, false.B )

  step(n)

  expect(m.io.done, false.B )
  expect(m.io.value, 4.U )

  step(1)

  expect(m.io.done, true.B )

}

class StallTest(m: Counter, n: Int) extends PeekPokeTester(m, n){

  poke(m.io.reset_, true.B )
  poke(m.io.input, n.U )
  poke(m.io.stall, false.B )

  step(2)

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

  step(1)

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, 0.U )
  poke(m.io.stall, false.B )

  step(1)

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, 1.U )
  poke(m.io.stall, false.B )

  step(1)

  expect(m.io.done, false.B )
  expect(m.io.value, 0.U )

  step(1)

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, 1.U )
  poke(m.io.stall, false.B )

  step(1)

  expect(m.io.done, false.B )
  expect(m.io.value, 0.U )

  step(1)

  expect(m.io.done, true.B )

  poke(m.io.reset_, true.B )
  poke(m.io.input, 1.U )
  poke(m.io.stall, true.B )

  step(4)

  expect(m.io.done, false.B )
  expect(m.io.value, 0.U )

  poke(m.io.stall, false.B )

  step(1)

  expect(m.io.done, true.B )







}

class CounterTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  /*
  behavior of "StateMachineTest"
  it should "Change state correctly when triggered" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new Counter_StateMachine()) {
      c => new StateMachineTest(c)
    } should be (true)
  }*/

  
  val n = 5

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
  
  
}