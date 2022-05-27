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



// the id says the first part of the address of the PE memory
// useful when doing load and stores
class Port(width: Int) extends Bundle{
    val in = Flipped(Decoupled(new Neighbour()))
    val out = Decoupled(new Neighbour())
}

class Connections(width: Int = 32) extends Bundle{
    val up = new Port(width) // id = 1
    val down = new Port(width) // id = 2
    val left = new Port(width) // id = 3
    val right = new Port(width) // id = 4
}


class PE( width: Int = 32,
         load_funct: Int = 0, store_funct: Int = 1, exchange_funct: Int = 3,
         up_memory_partial_address: Int = 1,
         down_memory_partial_address: Int = 2,
         left_memory_partial_address: Int = 3,
         right_memory_partial_address: Int = 4,
         ) extends Module{


    val io = IO(new Bundle{
        val cmd = Flipped(Decoupled(new Command)) // inputs : bits, valid (out : ready)
        val resp = Decoupled(new Response) // inputs : ready (out : bits, valid)
        val conn = new Connections(width)
    
    })

    val half_width = 16

    // renimimg

    val cmd = io.cmd
    val resp = io.resp
    val conn = io.conn

    val funct = cmd.bits.funct
    val rs1 = cmd.bits.rs1
    val rs2 = cmd.bits.rs2

    val data = resp.bits.data

    val left = conn.left
    val right = conn.right
    val up = conn.up
    val down = conn.down

    // util signals

    // funct count
    val is_load = funct === load_funct.U 
    val is_store = funct === store_funct.U 
    val is_exchange = funct === exchange_funct.U 

    // partial address managment
    val partial_address = rs2(half_width + 2, half_width)
    val is_main_memory = partial_address === 0.U 
    val is_up_memory = partial_address === up_memory_partial_address.U 
    val is_down_memory = partial_address === down_memory_partial_address.U 
    val is_left_memory = partial_address === left_memory_partial_address.U 
    val is_right_memory = partial_address === right_memory_partial_address.U 
    

    // counter signals
    val n = rs1(width - 1, half_width)

    // is my pe busy now ? <=> are my side memories storing values ?
    val busy = Wire(Bool())

    // memory main output
    val output_main_memory = Wire(Bits(half_width.W ))

    // memory up outputs
    val up_memory_busy = Wire(Bool())
    val output_up_memory = Wire(Bits(half_width.W ))

    // memory doen outputs
    val down_memory_busy = Wire(Bool())
    val output_down_memory = Wire(Bits(half_width.W ))

    // memory left outputs
    val left_memory_busy = Wire(Bool())
    val output_left_memory = Wire(Bits(half_width.W ))

    // memory right outputs
    val right_memory_busy = Wire(Bool())
    val output_right_memory = Wire(Bits(half_width.W ))

    // counter inputs
    val stall = Wire(Bool())

    // counter outputs
    val offset = Wire(Bits(half_width.W ))
    val count_done = Wire(Bool())


    // memories

    val main_memory = Module(new PE_memory(half_width))
    val up_memory = Module(new PE_SideMemory(half_width, up_memory_partial_address ))
    val down_memory = Module(new PE_SideMemory(half_width, down_memory_partial_address ))
    val left_memory = Module(new PE_SideMemory(half_width, left_memory_partial_address ))
    val right_memory = Module(new PE_SideMemory(half_width, right_memory_partial_address ))

    // counter

    val counter = Module(new Counter(half_width))

    counter.io.reset_ := cmd.valid && is_exchange && !busy
    counter.io.input := n
    counter.io.stall := stall

    offset := counter.io.value
    count_done := counter.io.done

    // source address calculator

    val src_address_calculator = Module(new SourceAddressCalculator(half_width))

    val src_address = Wire(Bits(half_width.W))

    src_address_calculator.io.address := rs1(half_width - 1, 0)
    src_address_calculator.io.offset := offset
    src_address_calculator.io.count_done := count_done

    src_address := src_address_calculator.io.rs1

    // destination address calculator

    val dest_address = Wire(Bits(half_width.W ))

    val dest_address_calculator = Module(new SourceAddressCalculator(half_width))

    dest_address_calculator.io.address := rs2(half_width - 1, 0)
    dest_address_calculator.io.offset := offset
    dest_address_calculator.io.count_done := count_done

    dest_address := dest_address_calculator.io.rs1


    // up queue

    val up_queue_input = Wire(Flipped(Decoupled(new Neighbour)))

    up_queue_input.valid := /*RegNext(*/!count_done//)
    up_queue_input.bits.address := /*RegNext(*/dest_address//)
    up_queue_input.bits.data := /*RegNext(*/main_memory.io.result//)



    val up_queue = Queue(up_queue_input, 3)

    conn.up.out <> up_queue 
    /* equivalent (in theory) to :

    conn.up.out.bits.data := up_queue.bits.data
    io.test := up_queue.bits.address
    conn.up.out.bits.address := io.test
    conn.up.out.valid := up_queue.valid
    up_queue.ready := conn.up.out.ready
    */

    // down queue

    val down_queue_input = Wire(Flipped(Decoupled(new Neighbour)))

    down_queue_input.valid := /*RegNext(*/!count_done//)
    down_queue_input.bits.address := /*RegNext(*/dest_address//)
    down_queue_input.bits.data := /*RegNext(*/main_memory.io.result//)

    val down_queue = Queue(down_queue_input, 3)

    down_queue <> conn.down.out
    /* equivalent (in theory) to :

    conn.down.out.bits.data := down_queue.bits.data
    conn.down.out.bits.address := down_queue.bits.address
    conn.down.out.valid := down_queue.valid
    down_queue.ready := conn.down.out.ready
    */
    
    // left queue output

    val left_queue_input = Wire(Flipped(Decoupled(new Neighbour)))

    left_queue_input.valid := /*RegNext(*/!count_done//)
    left_queue_input.bits.address := /*RegNext(*/dest_address//)
    left_queue_input.bits.data := /*RegNext(*/main_memory.io.result//)

    val left_queue = Queue(left_queue_input, 3)

    left_queue <> conn.left.out

    /* equivalent (in theory) to :

    conn.left.out.bits.data := left_queue.bits.data
    conn.left.out.bits.address := left_queue.bits.address
    conn.left.out.valid := left_queue.valid
    left_queue.ready := conn.left.out.ready
    */

    // right queue

    val right_queue_input = Wire(Flipped(Decoupled(new Neighbour)))

    right_queue_input.valid := /*RegNext(*/!count_done//)
    right_queue_input.bits.address := /*RegNext(*/dest_address//)
    right_queue_input.bits.data := /*RegNext(*/main_memory.io.result//)

    val right_queue = Queue(right_queue_input, 3)

    right_queue <> conn.right.out
    /* equivalent (in theory) to :

    conn.right.out.bits.data := right_queue.bits.data
    conn.right.out.bits.address := right_queue.bits.address
    conn.right.out.valid := right_queue.valid
    right_queue.ready := conn.right.out.ready
    */


    // the data exchange should stall when at leat 1 of the input
    // queues is full for whatever reason (even if it shouldn't happpen)
    stall := !(left_queue_input.ready && right_queue_input.ready && up_queue_input.ready && down_queue_input.ready)

    // this signal is fundamental, because says whether new commands can be accepted
    // basically if at least 1 side memory is still storing a value from a neighbour
    // for whatever reason this signal should be true
    busy := up_memory_busy && down_memory_busy && left_memory_busy && right_memory_busy

    // connecting signals to memories

    // main memory

    val main_mem_rs1 = Wire(Bits(half_width.W ))
    val main_mem_rs2 = Wire(Bits(half_width.W ))
    val main_mem_load = Wire(Bool())
    val main_mem_store = Wire(Bool())

    when(!count_done){
        main_mem_rs1 := 0.U 
        main_mem_rs2 := src_address
        main_mem_load := true.B 
        main_mem_store := false.B 
    }.elsewhen(cmd.valid && (rs2(18, 16) === 0.U) && !busy){
        main_mem_rs1 := rs1(half_width - 1, 0)
        main_mem_rs2 := rs2(half_width - 1, 0)
        main_mem_load := is_load
        main_mem_store := is_store
    }.otherwise{
        main_mem_rs1 := 0.U 
        main_mem_rs2 := 0.U 
        main_mem_load := false.B 
        main_mem_store := false.B  
    }

    main_memory.io.rs1 := main_mem_rs1
    main_memory.io.rs2 := main_mem_rs2
    main_memory.io.is_load := main_mem_load
    main_memory.io.is_store := main_mem_store

    output_main_memory := main_memory.io.result


    // up memory

    up_memory.io.rs1 := rs1
    up_memory.io.rs2 := rs2
    up_memory.io.is_load := is_load
    up_memory.io.is_store := is_store
    up_memory.io.busy := busy
    up_memory.io.neighbour <> conn.up.in
    up_memory.io.cmd_valid := cmd.valid

    up_memory_busy := up_memory.io.pe_busy
    output_up_memory := up_memory.io.op_result

    // down memeory

    down_memory.io.rs1 := rs1
    down_memory.io.rs2 := rs2
    down_memory.io.is_load := is_load
    down_memory.io.is_store := is_store
    down_memory.io.busy := busy
    down_memory.io.neighbour <> conn.down.in
    down_memory.io.cmd_valid := cmd.valid

    down_memory_busy := down_memory.io.pe_busy
    output_down_memory := down_memory.io.op_result

    // left memory

    left_memory.io.rs1 := rs1
    left_memory.io.rs2 := rs2
    left_memory.io.is_load := is_load
    left_memory.io.is_store := is_store
    left_memory.io.busy := busy
    left_memory.io.neighbour <> conn.left.in
    left_memory.io.cmd_valid := cmd.valid

    left_memory_busy := left_memory.io.pe_busy
    output_left_memory := left_memory.io.op_result

    // right memory

    right_memory.io.rs1 := rs1
    right_memory.io.rs2 := rs2
    right_memory.io.is_load := is_load
    right_memory.io.is_store := is_store
    right_memory.io.busy := busy
    right_memory.io.neighbour <> conn.right.in
    right_memory.io.cmd_valid := cmd.valid

    right_memory_busy := right_memory.io.pe_busy
    output_right_memory := right_memory.io.op_result

    // outputs of the module

    resp.valid := cmd.valid && is_load && !busy
    cmd.ready := resp.ready && !busy

    when(is_main_memory){
        resp.bits.data := output_main_memory
    }.elsewhen(is_up_memory){
        resp.bits.data := output_up_memory
    }.elsewhen(is_down_memory){
        resp.bits.data := output_down_memory
    }.elsewhen(is_left_memory){
        resp.bits.data := output_left_memory
    }.elsewhen(is_right_memory){
        resp.bits.data := output_right_memory
    }.otherwise{
        resp.bits.data := 0.U 
    }
}

