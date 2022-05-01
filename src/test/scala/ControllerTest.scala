
package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class CustomInterfaceControllerTest(queue_size: Int) extends Module{

    val io = IO(new Bundle{

        // command signals
        val cmd_bits_funct = Output(Bits(7.W))
        val cmd_bits_rs1 = Output(Bits(32.W))
        val cmd_bits_rs2 = Output(Bits(32.W))
        val cmd_ready = Input(Bool())
        val cmd_valid = Output(Bool())

        // resp signals
        val resp_bits_data = Input(Bits(32.W))
        val resp_valid = Input(Bool())
        val resp_ready = Output(Bool())


        // rocc signals
        val rocc_cmd_ready = Output(Bool())
        val rocc_cmd_valid = Input(Bool())

        val rocc_cmd_bits_inst_funct = Input(Bits(7.W))
        val rocc_cmd_bits_inst_rs2 = Input(Bits(5.W))
        val rocc_cmd_bits_inst_rs1 = Input(Bits(5.W))
        val rocc_cmd_bits_inst_xd = Input(Bool())
        val rocc_cmd_bits_inst_xs1 = Input(Bool())
        val rocc_cmd_bits_inst_xs2 = Input(Bool())
        val rocc_cmd_bits_inst_rd = Input(Bits(5.W))
        val rocc_cmd_bits_inst_opcode = Input(Bits(7.W))

        val rocc_cmd_bits_rs1 = Input(Bits(32.W))
        val rocc_cmd_bits_rs2 = Input(Bits(32.W))

        val rocc_resp_ready = Input(Bool())
        val rocc_resp_valid = Output(Bool())
        val rocc_resp_bits_rd = Output(Bits(5.W))
        val rocc_resp_bits_data = Output(Bits(32.W))

        val rocc_interrupt = Output(Bool())
        val rocc_busy = Output(Bool())

    })

    val contr = Module(new Controller(queue_size))

    // connecting inputs: contr = io.in

    contr.resp.bits.data := io.resp_bits_data
    contr.resp.valid := io.resp_valid
    contr.cmd.ready := io.cmd_ready

    contr.rocc.cmd.valid := io.rocc_cmd_valid
    contr.rocc.cmd.bits.inst.funct := io.rocc_cmd_bits_inst_funct
    contr.rocc.cmd.bits.inst.rs2 := io.rocc_cmd_bits_inst_rs2
    contr.rocc.cmd.bits.inst.rs1 := io.rocc_cmd_bits_inst_rs1
    contr.rocc.cmd.bits.rs2 := io.rocc_cmd_bits_rs2
    contr.rocc.cmd.bits.rs1 := io.rocc_cmd_bits_rs1
    contr.rocc.cmd.bits.inst.xd := io.rocc_cmd_bits_inst_xd
    contr.rocc.cmd.bits.inst.xs1 := io.rocc_cmd_bits_inst_xs1
    contr.rocc.cmd.bits.inst.xs2 := io.rocc_cmd_bits_inst_xs2
    contr.rocc.cmd.bits.inst.rd := io.rocc_cmd_bits_inst_rd
    contr.rocc.cmd.bits.inst.opcode := io.rocc_cmd_bits_inst_opcode
    contr.rocc.resp.ready := io.rocc_resp_ready

    // connecting outputs: io.out = contr

    io.rocc_busy := contr.rocc.busy
    io.cmd_bits_funct := contr.cmd.bits.funct
    io.cmd_bits_rs1 := contr.cmd.bits.rs1
    io.cmd_bits_rs2 := contr.cmd.bits.rs2
    io.cmd_valid := contr.cmd.valid
    io.resp_ready := contr.resp.ready
    io.rocc_cmd_ready := contr.rocc.cmd.ready
    io.rocc_resp_valid := contr.rocc.resp.valid
    io.rocc_resp_bits_rd := contr.rocc.resp.bits.rd
    io.rocc_resp_bits_data := contr.rocc.resp.bits.data
    io.rocc_interrupt := contr.rocc.interrupt
}



class InputQueueEmptyTest(m : CustomInterfaceControllerTest, n: Int) extends PeekPokeTester(m){

  // n = 3 by default in theory

  val i = 1

  // input: queue empty, send useless data
  poke(m.io.rocc_cmd_valid, false.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, false.B ) 

  // qi = [NULL, NULL, NULL]
  
  step(1) // 0 -> 1

  // qi = [NULL, NULL, NULL]

  // check if queue is still empty

  expect(m.io.cmd_valid, false.B ) // input queue empty
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  // input: queue empty, send useless data (again)
  poke(m.io.rocc_cmd_valid, false.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, true.B )

  // qi = [NULL, NULL, NULL]
  
  step(1) // 1 -> 2

  // qi = [NULL, NULL, NULL]

  // check if queue is still empty (again)

  expect(m.io.cmd_valid, false.B ) // input queue empty
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  // input: queue empty, send useful data

  poke(m.io.rocc_cmd_valid, true.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, false.B )

  // qi = [NULL, NULL, NULL]

  step(1) // 2-> 3

  // qi = [NULL, NULL, 1]

  // check if the queue has now meaninful data

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, i.U )
  expect(m.io.cmd_bits_rs2, i.U )
  expect(m.io.cmd_bits_funct, i.U )
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  // now empty the queue
  // input : queue with 1 value of 3, send useless data

  poke(m.io.rocc_cmd_valid, false.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, true.B )

  // qi = [NULL, NULL, 1]

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, i.U )
  expect(m.io.cmd_bits_rs2, i.U )
  expect(m.io.cmd_bits_funct, i.U )
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  step(1) // 3-> 4

  // qi = [NULL, NULL, NULL]

  // check if the queue now is empty again

  expect(m.io.cmd_valid, false.B ) // input queue empty
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full


}



class InputQueueFullTest(m : CustomInterfaceControllerTest, n: Int) extends PeekPokeTester(m){

  var i = 3

  // queue empty, fill it with some values


  poke(m.io.rocc_cmd_valid, true.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, false.B )

  // qi = [NULL, NULL, NULL]

  step(1)

  // qi = [NULL, NULL, 3] -> 3

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, i.U )
  expect(m.io.cmd_bits_rs2, i.U )
  expect(m.io.cmd_bits_funct, i.U )
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  i+= 1

  poke(m.io.rocc_cmd_valid, true.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, false.B )

  // qi = [NULL, NULL, 3] -> 3

  step(1)

  // qi = [NULL, 4, 3] -> 3

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 3.U )
  expect(m.io.cmd_bits_rs2, 3.U )
  expect(m.io.cmd_bits_funct, 3.U )
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  i+= 1

  poke(m.io.rocc_cmd_valid, true.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, false.B )

  // qi = [NULL, 4, 3]

  step(1)

  // qi = [5, 4, 3] -> 3

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 3.U )
  expect(m.io.cmd_bits_rs2, 3.U )
  expect(m.io.cmd_bits_funct, 3.U )
  expect(m.io.rocc_cmd_ready, false.B ) // input queue full

  i+= 1

  poke(m.io.rocc_cmd_valid, true.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, false.B )

  // 6 asks : qi = [5, 4, 3]

  step(1)

  // qi = [5, 4, 3] -> 3 : 6 was not enqueued because queue is full

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 3.U )
  expect(m.io.cmd_bits_rs2, 3.U )
  expect(m.io.cmd_bits_funct, 3.U )
  expect(m.io.rocc_cmd_ready, false.B ) // input queue full

  i+= 1

  poke(m.io.rocc_cmd_valid, true.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, true.B )

  // 7 asks : qi = [5, 4, 3]

  step(1)

  // qi = [NULL, 5, 4] -> 4 : 7 was rejected in the last CK because the queue was full, now the CK raises and then it shifts the queue elements

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 4.U )
  expect(m.io.cmd_bits_rs2, 4.U )
  expect(m.io.cmd_bits_funct, 4.U )
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full
  
  poke(m.io.rocc_cmd_valid, true.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, false.B )

  // 7 asks : qi = [NULL, 5, 4]

  step(1)

  // qi = [7, 5, 4] -> 4 : 7 was accpted because in the last CK there was a free space

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 4.U )
  expect(m.io.cmd_bits_rs2, 4.U )
  expect(m.io.cmd_bits_funct, 4.U )
  expect(m.io.rocc_cmd_ready, false.B ) // input queue full

  i+= 1

  poke(m.io.rocc_cmd_valid, false.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, true.B )

  // qi = [7, 5, 4] 

  step(1)

  // qi = [NULL, 7, 5] -> 5

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 5.U )
  expect(m.io.cmd_bits_rs2, 5.U )
  expect(m.io.cmd_bits_funct, 5.U )
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  i+= 1

  poke(m.io.rocc_cmd_valid, true.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, false.B )

  // 9 asks : qi = [NULL, 7, 5] 

  step(1)

  // qi = [9, 7, 5] -> 5

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 5.U )
  expect(m.io.cmd_bits_rs2, 5.U )
  expect(m.io.cmd_bits_funct, 5.U )
  expect(m.io.rocc_cmd_ready, false.B ) // input queue full

  poke(m.io.rocc_cmd_valid, false.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, true.B )

  // qi = [9, 7, 5] 

  step(1)

  // qi = [NULL, 9, 7] -> 7

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 7.U )
  expect(m.io.cmd_bits_rs2, 7.U )
  expect(m.io.cmd_bits_funct, 7.U )
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  poke(m.io.rocc_cmd_valid, false.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, true.B )

  // qi = [NULL, 7, 9] 

  step(1)

  // qi = [NULL, NULL, 9] -> 9

  expect(m.io.cmd_valid, true.B ) // input queue not empty
  expect(m.io.cmd_bits_rs1, 9.U )
  expect(m.io.cmd_bits_rs2, 9.U )
  expect(m.io.cmd_bits_funct, 9.U )
  expect(m.io.rocc_cmd_ready, true.B ) // input queue not full

  poke(m.io.rocc_cmd_valid, false.B )
  poke(m.io.rocc_cmd_bits_inst_funct, i.U )
  poke(m.io.rocc_cmd_bits_rs1, i.U )
  poke(m.io.rocc_cmd_bits_rs2, i.U )
  poke(m.io.cmd_ready, true.B )

  // qi = [NULL, NULL, 9] 

  step(1)

  // qi = [NULL, NULL, NULL] -> ??

  expect(m.io.cmd_valid, false.B ) // input queue empty
  expect(m.io.rocc_cmd_ready, true.B ) // input queue NOT full

}

class OutputQueueEmptyTest(m : CustomInterfaceControllerTest, n: Int) extends PeekPokeTester(m){

  var i = 1
  // qout: [NULL, NULL, NULL]

  expect(m.io.resp_ready, true.B ) // the queue is NOT full (tis signal, differently from the valid from rocc, always represents the real queue status)
  expect(m.io.rocc_resp_valid, true.B ) // here, normally it SHOULD be false, but since the controller works in such a way that the queue is activated toward the rocc response interface only if valid === 1 && funct === 2.U, this is true

  poke(m.io.resp_valid, false.B )
  poke(m.io.resp_bits_data, i.U )
  poke(m.io.rocc_resp_ready, true.B )
  poke(m.io.rocc_cmd_valid, true.B ) // this with rocc_cmd_funct allows the output queue to be activated
  poke(m.io.rocc_cmd_bits_inst_funct, 2.U ) // this with rocc_cmd_valid allows the output queue to be activated

  // qout: [NULL, NULL, NULL]

  step(1)

  // qout: ?? <- [NULL, NULL, NULL]

  //expect(m.io.rocc_resp_bits_data, i.U )
  expect(m.io.resp_ready, true.B ) // the queue is NOT full
  expect(m.io.rocc_resp_valid, false.B ) // the queue is empty
  


}

class OutputQueueFullTest(m : CustomInterfaceControllerTest, n: Int) extends PeekPokeTester(m){


  
}

class InputQueueStallTest(m : CustomInterfaceControllerTest, n: Int) extends PeekPokeTester(m){



}


class OutputQueueStallTest(m : CustomInterfaceControllerTest, n: Int) extends PeekPokeTester(m){



}






class ControllerTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  val n = 3

  behavior of "InputQueueEmptyTest"
  it should "Enqueue and behave correctly when goes from empty to (not completely) full, from ROCC CMD" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest(n)) {
      c => new InputQueueEmptyTest(c, n)
    } should be (true)
  } 

  behavior of "InputQueueFullTest"
  it should "Enqueue and behave correctly when goes from (not completely) to completely full, from ROCC CMD" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest(n)) {
      c => new InputQueueFullTest(c, n)
    } should be (true)
  } 

  behavior of "OutputQueueEmptyTest"
  it should "Enqueue and behave correctly when goes from empty to completely (not completely) full, from PE RESP" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest(n)) {
      c => new OutputQueueEmptyTest(c, n)
    } should be (true)
  }

  behavior of "OutputQueueFullTest"
  it should "Enqueue and behave correctly when goes from (not completely) to completely full, from PE RESP" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest(n)) {
      c => new OutputQueueFullTest(c, n)
    } should be (true)
  }

  behavior of "OutputQueueStallTest"
  it should "Stall and store the current values when the funct is not get_load(when rocc_cmd_bits_inst_funct != 2.U )" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest(n)) {
      c => new OutputQueueStallTest(c, n)
    } should be (true)
  }

  behavior of "InputQueueStallTest"
  it should "Stall and store the current values when the funct is get_load(when rocc_cmd_bits_inst_funct === 2.U )" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest(n)) {
      c => new OutputQueueStallTest(c, n)
    } should be (true)
  }



}

