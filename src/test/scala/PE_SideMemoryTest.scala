package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class WrongLoadAndStoreTest(m: PE_SideMemory, id : Int) extends PeekPokeTester(m, id){

    val result_store = 100
    val result_error = 50

    // this conditions holding for all the execution in order to receive a real load/store from the controller
    //, no neighbour priority
    poke(m.io.neighbour.valid, false.B ) // no priority to neighbour requests
    poke(m.io.cmd_valid, true.B ) // command received is valid
    poke(m.io.busy, false.B ) // pe is not receiving anywhere else neighbour requests

    poke(m.io.rs2, 2.U )
    poke(m.io.rs1, 4.U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    step(1)

    expect(m.io.pe_busy, false.B )
    expect(m.io.op_result, result_error.U )

    poke(m.io.rs2, (2 + (id << 16)).U )
    poke(m.io.rs1, 4.U )
    poke(m.io.is_load, false.B )
    poke(m.io.is_store, true.B )

    step(1)

    expect(m.io.pe_busy, false.B )
    expect(m.io.op_result, result_store.U )

    poke(m.io.rs2, 4.U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    step(1)

    expect(m.io.pe_busy, false.B )
    expect(m.io.op_result, result_error.U )    

    poke(m.io.rs2, (2 + (id << 16)).U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    step(1)

    expect(m.io.pe_busy, false.B )
    expect(m.io.op_result, 4.U )

}

class NeighbourPrecedenceTest(m: PE_SideMemory, id : Int) extends PeekPokeTester(m, id){

    val result_store = 100
    
    poke(m.io.cmd_valid, true.B ) // command received is valid
    poke(m.io.busy, false.B ) // pe is not receiving anywhere else neighbour requests

    // from main PE
    poke(m.io.rs2, (2 + (id << 16)).U )
    poke(m.io.rs1, 4.U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    // from Neighbour
    poke(m.io.neighbour.valid, true.B )
    poke(m.io.neighbour.bits.address, 5.U )
    poke(m.io.neighbour.bits.data, 10.U )

    step(1)

    // ideally it should give precedence to the neighbour request and do the store

    expect(m.io.pe_busy, true.B )
    expect(m.io.op_result, result_store.U)

    // now verify if the store has succeeded

    // from main PE
    poke(m.io.rs2, (5 + (id << 16)).U )
    poke(m.io.rs1, 4.U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    // from Neighbour
    poke(m.io.neighbour.valid, false.B )
    poke(m.io.neighbour.bits.address, 5.U )
    poke(m.io.neighbour.bits.data, 10.U )

    step(1)

    expect(m.io.pe_busy, false.B )
    expect(m.io.op_result, 10.U )

    // from main PE
    poke(m.io.rs2, (5 + (id << 16)).U )
    poke(m.io.rs1, 7.U )
    poke(m.io.is_load, false.B )
    poke(m.io.is_store, true.B )

    // from Neighbour
    poke(m.io.neighbour.valid, false.B )
    poke(m.io.neighbour.bits.address, 5.U )
    poke(m.io.neighbour.bits.data, 10.U )

    step(1)

    expect(m.io.pe_busy, false.B )
    expect(m.io.op_result, result_store.U )

    // from main PE
    poke(m.io.rs2, (5 + (id << 16)).U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    // from Neighbour
    poke(m.io.neighbour.valid, true.B )
    poke(m.io.neighbour.bits.address, 10.U )
    poke(m.io.neighbour.bits.data, 20.U )

    step(1)

    expect(m.io.pe_busy, true.B )
    expect(m.io.op_result, result_store.U )

    // from main PE
    poke(m.io.rs2, (5 + (id << 16)).U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    // from Neighbour
    poke(m.io.neighbour.valid, false.B )
    poke(m.io.neighbour.bits.address, 10.U )
    poke(m.io.neighbour.bits.data, 20.U )

    step(1)

    expect(m.io.pe_busy, false.B )
    expect(m.io.op_result, 7.U )

    // from main PE
    poke(m.io.rs2, (10 + (id << 16)).U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    // from Neighbour
    poke(m.io.neighbour.valid, false.B )
    poke(m.io.neighbour.bits.address, 10.U )
    poke(m.io.neighbour.bits.data, 20.U )

    step(1)

    expect(m.io.pe_busy, false.B )
    expect(m.io.op_result, 20.U )
}

class BusyValidTest(m: PE_SideMemory, id : Int) extends PeekPokeTester(m, id){

    val result_error = 50
    val result_store = 100
    val result_load = 5

    poke(m.io.neighbour.valid, false.B ) // no priority to neighbour requests

    // quick store to verify result later
    poke(m.io.rs2, (2+ (id << 16)).U )
    poke(m.io.rs1, result_load.U )
    poke(m.io.is_load, false.B )
    poke(m.io.is_store, true.B )

    poke(m.io.busy, false.B ) // GOOD
    poke(m.io.cmd_valid, true.B ) // BAD

    step(1)

    poke(m.io.rs2, (2 + (id << 16)).U )
    poke(m.io.rs1, 4.U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    poke(m.io.busy, false.B ) // GOOD
    poke(m.io.cmd_valid, false.B ) // BAD

    step(1)

    expect(m.io.op_result, result_error.U) // GOOD && BAD = BAD

    poke(m.io.rs2, (2 + (id << 16)).U )
    poke(m.io.rs1, 4.U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    poke(m.io.busy, true.B ) // BAD
    poke(m.io.cmd_valid, true.B ) // GOOD

    step(1)

    expect(m.io.op_result, result_error.U) // BAD && GOOD = BAD

    poke(m.io.rs2, (2 + (id << 16)).U )
    poke(m.io.rs1, 4.U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    poke(m.io.busy, false.B ) // GOOD
    poke(m.io.cmd_valid, true.B ) // GOOD

    step(1)

    expect(m.io.op_result, result_load.U) // GOOD && GOOD = GOOD

    poke(m.io.neighbour.valid, true.B ) // no priority to neighbour requests

    poke(m.io.rs2, (2 + (id << 16)).U )
    poke(m.io.rs1, 4.U )
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    poke(m.io.busy, true.B ) // BAD
    poke(m.io.cmd_valid, false.B ) // BAD

    step(1)

    expect(m.io.op_result, result_store.U) // neighbour gets precedence


}


class PE_SideMemoryTests extends ChiselFlatSpec {

  val testerArgs = Array("")

  val id = 2
  val width = 16

  behavior of "WrongLoadAndStoreTest"
  it should "Do anything if the commands of load and stores are not for this specific memory" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE_SideMemory(16, id)) {
      c => new WrongLoadAndStoreTest(c, id)
    } should be (true)
  }


  behavior of "NeighbourPrecedenceTest"
  it should "Always give priority to neighbour requests" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE_SideMemory(16, id)) {
      c => new NeighbourPrecedenceTest(c, id)
    } should be (true)
  }

  behavior of "BusyValidTest"
  it should "Not perform any operation when at least one between !busy and cmd_valid is true" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE_SideMemory(16, id)) {
      c => new BusyValidTest(c, id)
    } should be (true)
  }


  
}