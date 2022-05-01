package torus

import chisel3._
import chisel3.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.config.{Config, Parameters}
import chisel3.util.HasBlackBoxResource
import chisel3.experimental.IntParam
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.InOrderArbiter


class Port(width: Int) extends Bundle{
    val in = Input(Bits(width.W))
    val out = Output(Bits(width.W))
}

class Connections(width: Int = 32) extends Bundle{
    //val up = new Port(width)
    //val down = new Port(width)
    val left = new Port(width)
    val right = new Port(width)
    //val ingoing = new Port(width)
    //val outgoing = new Port(width)
}


class PE(id: Int) extends Module{
    val io = IO(new Bundle{
        val cmd = Flipped(Decoupled(new Command)) // inputs : bits, valid (out : ready)
        val resp = Decoupled(new Response) // inputs : ready (out : bits, valid)
        val conn = new Connections
        //val was_store = Output(Bool())
    })

    //register memory with some custom funcionalities (load immediate, store immediate)
    val memory = Module(new PE_memory())

    // buffer in which storing the result to return
    // needed because:
        // 1) the controller expects to recive the result in the next clock cycle
        // 2) if the controller cannot recive the result (resp_ready = false) we have to keep it until it can
    val keep_reg = Module(new KeepReg(32))

    // buffer in which storing the operation that was issued by the result
    // needed because if this is a store, it's not actually needed to give a valid = true as
    // output since the response will be asked on demand in the controller when funct === 2.U
    val keep_funct = Module(new KeepReg(7))

    // stores the result of the operation
    val op_result = Wire(Bits(32.W))

    // this value here says whether the return register has to keep its value or accept new values
    val keep = Wire(Bool())

//----------------------START STATE MACHINE-----------------------------

    // see doc on the state machine defined below
    val state_machine = Module(new PE_control_unit)

    state_machine.io.cmd_valid := io.cmd.valid
    state_machine.io.resp_ready := io.resp.ready
    io.cmd.ready := state_machine.io.cmd_ready
    //io.resp.valid := state_machine.io.resp_valid
    keep := state_machine.io.keep


//----------------------END STATE MACHINE-----------------------------

    val rs1 = io.cmd.bits.rs1
    val rs2 = io.cmd.bits.rs2

 //----------------------START DATAPATH-----------------------------


    // conventions
    // load: funct == 0.U , result := mem(rs1)
    // store: funct == 1.U, mem(rs2) := rs1

    // decide which operation to do with the funct bits
    val is_load = io.cmd.bits.funct === 0.U 
    val is_store = io.cmd.bits.funct === 1.U 

    // connecting the inputs to the PE memory
    memory.io.rs1 := io.cmd.bits.rs1
    memory.io.rs2 := io.cmd.bits.rs2
    memory.io.is_load := is_load
    memory.io.is_store := is_store

    // connecting the output to the PE memory
    op_result := memory.io.result

    // connecting inputs to keep_register and keep_funct (they work in parallel)
    keep_reg.io.keep := keep
    keep_reg.io.in := op_result
    keep_funct.io.in := io.cmd.bits.funct
    keep_funct.io.keep := keep

    // connecting register to output of keep_register and keep_funct
    io.resp.bits.data := keep_reg.io.out
    //io.was_store := keep_funct.io.out === 1.U 
 
    // default, in future they will change
    io.conn.right.out := 0.U 
    io.conn.left.out := 0.U 

    // another possible solution may be :
    
    
    io.resp.valid := state_machine.io.resp_valid && !keep_funct.io.out === 1.U
    
    
    

 //----------------------END DATAPATH-----------------------------

}



/*
this  module is the state machine of the PE, it has 2 states, but
in practice it's like they are 3 :
    reg_free: an idle state in which it waits for valid input
    full_reg && resp_ready : a state in which I can return a computed result and eventually pipeline my execution if there are new inputs available
    full_reg && !resp_ready : a state in which I have to wait until my resp is ready to be issued (of course I cannot accept new inputs here)

*/
class PE_control_unit extends Module{

    val io = IO(new Bundle{

        val cmd_ready = Output(Bool()) // am I ready to receive new inputs?
        val cmd_valid = Input(Bool()) // is the new input received valid?
        val resp_ready = Input(Bool()) // is my output interface ready to receive commands?
        val resp_valid = Output(Bool()) // am I able to give a valid output in this cycle?
        val keep = Output(Bool()) // should I buffer my result or keep the execution flowing?

    })

    // renaming
    val cmd_ready = io.cmd_ready
    val cmd_valid = io.cmd_valid
    val resp_ready = io.resp_ready
    val resp_valid = io.resp_valid
    val keep = io.keep

    val reg_free :: full_reg :: full_reg_stall :: end_pipeline:: Nil = Enum(4)

    val state = Reg(Bits(3.W))
    
    state := reg_free

    when(state === reg_free){

        keep := false.B 
        cmd_ready := true.B 
        resp_valid := false.B 

        when(!cmd_valid){
            state := reg_free
        }.elsewhen(cmd_valid && resp_ready){
            state := full_reg
        }.otherwise{ // cmd_valid && !resp_ready
            state := full_reg_stall
        }

    }.elsewhen(state === full_reg){

        keep := false.B 
        cmd_ready := true.B 
        resp_valid := true.B 
        
        when(!resp_ready){
            state := full_reg_stall
        }.elsewhen(cmd_valid && resp_ready){
            state := full_reg
        }.otherwise{ // cmd_valid && resp_ready
            state := end_pipeline
        }

    }.elsewhen(state === full_reg_stall){

        keep := true.B 
        cmd_ready := false.B 
        resp_valid := true.B 

        when(!resp_ready){
            state := full_reg_stall
        }.elsewhen(resp_ready && cmd_valid){
            state := full_reg
        }.otherwise{ // resp_ready && !cmd_valid
            state := end_pipeline
        }
        

    }.elsewhen(state === end_pipeline){
        keep := false.B 
        cmd_ready := true.B 
        resp_valid := true.B 
        state := reg_free
    
    }.otherwise{ // it should never happen
        io.cmd_ready := false.B 
        io.resp_valid := false.B
        io.keep := true.B 
        state := reg_free
    }


}


/*
this module executes basic memory operations:
    load: when is_load === true
    store: when is_store === true

    if is_load === true and is_store === true the behaviour is udefined
*/
class PE_memory(width : Int = 32) extends Module{

    val io = IO(new Bundle{
        val rs1 = Input(Bits(width.W))
        val rs2 = Input(Bits(width.W))
        val is_load = Input(Bool())
        val is_store = Input(Bool())
        val result = Output(Bits(width.W))
    })

    // conventions
    // load: funct == 0.U , result := mem(rs1)
    // store: funct == 1.U, mem(rs2) := rs1

    //register memory
    val reg_memory = Mem(width, UInt(width.W))

    // renaming
    val rs1 = io.rs1
    val rs2 = io.rs2
    val is_load = io.is_load
    val is_store = io.is_store
    val result = io.result
    
    when(is_load){ // does a load
        result := reg_memory(rs1)
    }.elsewhen(is_store){ // does a store
        reg_memory(rs2) := rs1
        result := 100.U // assuming width >= 7
    }.otherwise{
        result := 0.U 
    }

}