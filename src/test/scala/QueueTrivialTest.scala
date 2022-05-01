package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._

//////////////////////////////// TEST ON QUEUE TO UNDERSTAND IT...IGNORE THIS//////////////////////////////

class QueueInterfaceTest extends Module{
  val io = IO(new Bundle {
      val in = Flipped(Decoupled(UInt(8.W))) // inputs : bits, valid (out : ready)
      val out = Decoupled(UInt(8.W)) // inputs : ready (out : bits, valid)
    })
    val queue = Queue(io.in, 2)  // 2-element queue
    io.out <> queue
}

class Queue_test(mo: QueueInterfaceTest) extends PeekPokeTester(mo){

  poke(mo.io.in.valid, true.B )
  poke(mo.io.in.bits, 20.U )
  poke(mo.io.out.ready, false.B )

  // q = [ NULL, NULL]

  step(1)

  // q = [NULL, 20] -> 20

  expect(mo.io.in.ready, true.B ) // the queue is NOT full
  expect(mo.io.out.valid, true.B ) // the queue is NOT empty
  expect(mo.io.out.bits, 20.U )

  poke(mo.io.in.valid, true.B )
  poke(mo.io.in.bits, 40.U )
  poke(mo.io.out.ready, false.B )

  step(1)

  // q = [40, 20] -> 20

  expect(mo.io.in.ready, false.B ) // the queue is FULL
  expect(mo.io.out.valid, true.B ) // the queue is NOT empty
  expect(mo.io.out.bits, 20.U )

  poke(mo.io.in.valid, true.B )
  poke(mo.io.in.bits, 60.U )
  poke(mo.io.out.ready, false.B )

  step(1)

  // q = [40, 20] -> 20

  expect(mo.io.in.ready, false.B ) // the queue is FULL
  expect(mo.io.out.valid, true.B ) // the queue is NOT empty
  expect(mo.io.out.bits, 20.U )

  poke(mo.io.in.valid, false.B )
  poke(mo.io.in.bits, 80.U )
  poke(mo.io.out.ready, true.B )

  step(1)

  // q = [NULL, 40] -> 40

  expect(mo.io.in.ready, true.B ) // the queue is NOT empty
  expect(mo.io.out.valid, true.B ) // the queue is NOT empty
  expect(mo.io.out.bits, 40.U )

  poke(mo.io.in.valid, false.B )
  poke(mo.io.in.bits, 80.U )
  poke(mo.io.out.ready, true.B )

  step(1)

  // q = [NULL, NULL] -> ??

  expect(mo.io.in.ready, true.B ) // the queue is NOT full
  expect(mo.io.out.valid, false.B ) // the queue is EMPTY
  //expect(mo.io.out.bits, 0.U ) not valid => does not make sense to check the content

  poke(mo.io.in.valid, true.B )
  poke(mo.io.in.bits, 80.U )
  poke(mo.io.out.ready, true.B )

  // q = [NULL, NULL] 

  step(1)

  // q = [NULL, 80] -> 80

  expect(mo.io.in.ready, true.B ) // the queue is NOT full
  expect(mo.io.out.valid, true.B ) // the queue is NOT empty
  expect(mo.io.out.bits, 80.U )

  poke(mo.io.in.valid, true.B )
  poke(mo.io.in.bits, 30.U )
  poke(mo.io.out.ready, false.B )

  // q = [NULL, 80]

  step(1)

  // q = [30, 80] -> 80

  expect(mo.io.in.ready, false.B ) // the queue is full
  expect(mo.io.out.valid, true.B ) // the queue is NOT empty
  expect(mo.io.out.bits, 80.U )

  poke(mo.io.in.valid, true.B )
  poke(mo.io.in.bits, 70.U )
  poke(mo.io.out.ready, true.B )

  expect(mo.io.in.ready, false.B ) // the queue is full
  expect(mo.io.out.valid, true.B ) // the queue is NOT empty
  expect(mo.io.out.bits, 80.U )  

  // q = [30, 80] 

  step(1)

  // q = [NULL, 30] -> 30

  expect(mo.io.in.ready, true.B ) // the queue is NOT full
  expect(mo.io.out.valid, true.B ) // the queue is NOT empty
  expect(mo.io.out.bits, 30.U )  

}


class QueueTrivialTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  val n = 3

  behavior of "Queue_test"
  it should "Enqueue and Dequeue Correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new QueueInterfaceTest()) {
      c => new Queue_test(c)
    } should be (true)
  }  
}