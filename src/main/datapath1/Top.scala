package datapath1
import chisel3._
class Top extends Module{
  val io = IO(new Bundle{

    val instruction = Output(UInt(32.W))
    val AluOut = Output(SInt(32.W))
    val branchCheck = Output(UInt(1.W))
  })
  val control = Module(new Control)
  val imm = Module(new ImmediateGeneration)
  val aluCtrl = Module(new AluControl)
  val alu = Module(new Alu)
  val reg = Module(new RegisterFile)
  val InsMem = Module(new InstructionMem)
  val PC = Module(new Pc)
  val jalr = Module(new Jalr())
  val dataMem = Module(new DataMem)
  val IF_ID = Module(new IF_ID)
  val ID_EXE = Module(new ID_EXE)
  val EXE_MEM = Module(new EXE_MEM)
  val MEM_WB = Module(new MEM_WB)
  val forwarding = Module(new Forwarding)
  val branch = Module(new BranchUnit)
  val hazard_unit = Module(new HazardDetect)
  val branchForward = Module(new BranchForwarding)

 //Pc 
  PC.io.in := PC.io.pc4


  //Instruction Memory
  InsMem.io.wrAddr := PC.io.pc(11,2)
  io.instruction := InsMem.io.readData

  //IF_ID----in----
 IF_ID.io.pc_in := PC.io.pc
 IF_ID.io.pc4_in := PC.io.pc4
 IF_ID.io.ins_in := io.instruction

  //Control
  control.io.opcode := IF_ID.io.ins_out(6,0) //InsMem.io.rdData(6,0)

  //Immediate Generation
  imm.io.instruction := IF_ID.io.ins_out
  imm.io.pc :=  IF_ID.io.pc_out

  //RegisterFile 
  reg.io.rs1_sel := IF_ID.io.ins_out(19,15)
  reg.io.rs2_sel := IF_ID.io.ins_out(24,20)


  //Alu Control 
  aluCtrl.io.aluOp := control.io.out_aluOp
  aluCtrl.io.func3 := IF_ID.io.ins_out(14,12)
  aluCtrl.io.func7 := IF_ID.io.ins_out(30)

  //ALU 
  when(control.io.out_operand_a_sel === 0.U || control.io.out_operand_a_sel === 3.U){
    ID_EXE.io.operandA_in := reg.io.rs1
  }.elsewhen(control.io.out_operand_a_sel === 2.U){
    ID_EXE.io.operandA_in := IF_ID.io.pc4_out.asSInt //+ 4.U).asSInt
  }.elsewhen(control.io.out_operand_a_sel === 1.U){ 
    ID_EXE.io.operandA_in := IF_ID.io.pc_out.asSInt
  }.otherwise{
    ID_EXE.io.operandA_in := DontCare //alu.io.a
  }

  when(control.io.out_operand_b_sel === 0.U){
    ID_EXE.io.operandB_in := reg.io.rs2
  }.otherwise{
    when(ID_EXE.io.ExtendSel_Out === 0.U){
      ID_EXE.io.operandB_in := imm.io.i_imm
    }.elsewhen(ID_EXE.io.ExtendSel_Out === 2.U){
      ID_EXE.io.operandB_in := imm.io.s_imm
    }.elsewhen(ID_EXE.io.ExtendSel_Out === 1.U){
      ID_EXE.io.operandB_in := imm.io.u_imm
    }.otherwise{
      ID_EXE.io.operandB_in := reg.io.rs2 //DontCare
    }
  }

  //ID_EXE------in--------


  ID_EXE.io.opr_A_sel_in := control.io.out_operand_a_sel
  ID_EXE.io.opr_B_sel_in := control.io.out_operand_b_sel
  ID_EXE.io.rs1_sel_in := IF_ID.io.ins_out(19,15)
  ID_EXE.io.rs2_sel_in := IF_ID.io.ins_out(24,20)
  ID_EXE.io.memWrite_in := control.io.out_memWrite
  ID_EXE.io.memRead_in := control.io.out_memRead
  ID_EXE.io.memToReg_in := control.io.out_memToReg
  ID_EXE.io.rd_in := IF_ID.io.ins_out(11,7)
  ID_EXE.io.strData_in := reg.io.rs2
  ID_EXE.io.aluCtrl_in := aluCtrl.io.output
  ID_EXE.io.regWrite_in := control.io.out_regWrite
  ID_EXE.io.branch_In := control.io.out_branch
  ID_EXE.io.Aluop_In := control.io.out_aluOp
  ID_EXE.io.NextPcSel_In := control.io.out_next_pc_sel
  ID_EXE.io.ExtendSel_In := control.io.out_extend_sel
  

 //Branching

	branch.io.rs1 := reg.io.rs1
	branch.io.rs2 := reg.io.rs2
	branch.io.func3 := IF_ID.io.ins_out(14,12)

 //stalling
	
	hazard_unit.io.ID_EX_rdregister_In := ID_EXE.io.rd_out
	hazard_unit.io.IF_ID_instruction_In := IF_ID.io.ins_out
	hazard_unit.io.ID_EX_memRead_In := ID_EXE.io.memRead_out
	hazard_unit.io.pc_In := IF_ID.io.pc4_out
	hazard_unit.io.curr_pc_In := IF_ID.io.pc_out



	when(hazard_unit.io.ins_forward === "b1".U){
	 IF_ID.io.ins_in := hazard_unit.io.IF_ID_instruction_Out
	 IF_ID.io.pc_in := hazard_unit.io.curr_pc_Out
	}.otherwise{
	 IF_ID.io.ins_in := InsMem.io.readData
	}


	when(hazard_unit.io.pc_forward === "b1".U){
		PC.io.in := hazard_unit.io.pc_Out
	}.otherwise{
		when(control.io.out_next_pc_sel === "b01".U){
			when(control.io.out_branch === 1.U && branch.io.output === 1.U){
				   PC.io.in  := imm.io.sb_imm.asUInt
			 IF_ID.io.pc_in := 0.U
			 IF_ID.io.pc4_in := 0.U
			 IF_ID.io.ins_in := 0.U
			}.otherwise{
				PC.io.in := PC.io.pc4
			}
		}.elsewhen(control.io.out_next_pc_sel === "b10".U){
			PC.io.in  := imm.io.uj_imm.asUInt
		 IF_ID.io.pc_in := 0.U
		 IF_ID.io.pc4_in := 0.U
		 IF_ID.io.ins_in := 0.U
		}.elsewhen(control.io.out_next_pc_sel === "b11".U){
			PC.io.in  := jalr.io.output.asUInt
		 IF_ID.io.pc_in := 0.U
		 IF_ID.io.pc4_in := 0.U
		 IF_ID.io.ins_in := 0.U
		}.otherwise{
			PC.io.in := PC.io.pc4
		}
	}

	when(hazard_unit.io.out === 1.U){
		ID_EXE.io.memWrite_in := 0.U
		ID_EXE.io.memRead_in := 0.U
		ID_EXE.io.memToReg_in := 0.U
		ID_EXE.io.regWrite_in := 0.U
		ID_EXE.io.NextPcSel_In := 0.U
		ID_EXE.io.opr_A_sel_in := 0.U
		ID_EXE.io.opr_A_sel_in := 0.U
		ID_EXE.io.branch_In := 0.U
		ID_EXE.io.Aluop_In := 0.U
		PC.io.in := PC.io.pc4
		
	}.otherwise{
		ID_EXE.io.opr_A_sel_in := control.io.out_operand_a_sel
  		ID_EXE.io.opr_B_sel_in := control.io.out_operand_b_sel
  		ID_EXE.io.memWrite_in := control.io.out_memWrite
  		ID_EXE.io.memRead_in := control.io.out_memRead
  		ID_EXE.io.memToReg_in := control.io.out_memToReg
  		ID_EXE.io.regWrite_in := control.io.out_regWrite
  		ID_EXE.io.branch_In := control.io.out_branch
  		ID_EXE.io.Aluop_In := control.io.out_aluOp
  		ID_EXE.io.NextPcSel_In := control.io.out_next_pc_sel
	}


  //EXE
  alu.io.aluCtrl := ID_EXE.io.aluCtrl_out
  alu.io.oper_a := ID_EXE.io.operandA_out
  alu.io.oper_b := ID_EXE.io.operandB_out
  EXE_MEM.io.alu_Output_input := alu.io.out
  EXE_MEM.io.alu_branch_output_input := alu.io.branch
  io.AluOut := EXE_MEM.io.alu_Output_output //alu.io.aluOut
  io.branchCheck := EXE_MEM.io.alu_branch_output_output //alu.io.branch

  //EXE_MEM------in------
  EXE_MEM.io.rs2_in := ID_EXE.io.operandB_out
  EXE_MEM.io.rs1_in := ID_EXE.io.operandA_out
  EXE_MEM.io.rs1_sel_in := ID_EXE.io.rs1_sel_Out
  EXE_MEM.io.rs2_sel_in := ID_EXE.io.rs2_sel_Out
  EXE_MEM.io.memWrite_in := ID_EXE.io.memWrite_out
  EXE_MEM.io.memRead_in := ID_EXE.io.memRead_out
  EXE_MEM.io.memToReg_in := ID_EXE.io.memToReg_out
  EXE_MEM.io.rd_in := ID_EXE.io.rd_out
  EXE_MEM.io.strData_in := EXE_MEM.io.rs2_out
  EXE_MEM.io.alu_Output_input := alu.io.out
  EXE_MEM.io.regWrite_in := ID_EXE.io.regWrite_out



  //Data-Memory
  dataMem.io.memWrite := EXE_MEM.io.memWrite_out
  dataMem.io.memRead := EXE_MEM.io.memRead_out
  dataMem.io.memAddress := EXE_MEM.io.alu_Output_output(9,2).asUInt
  dataMem.io.memData := EXE_MEM.io.rs2_out //reg.io.rs2


  //MEM_WR----------in-------

  MEM_WB.io.rs1_sel_in := EXE_MEM.io.rs1_sel_Out
  MEM_WB.io.rs2_sel_in := EXE_MEM.io.rs2_sel_Out
  MEM_WB.io.memToReg_in := EXE_MEM.io.memToReg_out
  MEM_WB.io.rd_in := EXE_MEM.io.rd_out
  MEM_WB.io.aluOutput_in := EXE_MEM.io.alu_Output_output
  MEM_WB.io.dataOut_in := dataMem.io.memOut
  MEM_WB.io.regWrite_in := EXE_MEM.io.regWrite_out
  MEM_WB.io.memRead_in := EXE_MEM.io.memRead_out

  reg.io.rd_sel := MEM_WB.io.rd_out
  reg.io.regWrite := MEM_WB.io.regWrite_out

  when(MEM_WB.io.memToReg_out === 1.U){
    reg.io.writeData :=  MEM_WB.io.dataOut_out
  }.otherwise{
     reg.io.writeData := MEM_WB.io.aluOutput_out
  }


//forwarding hazard
  forwarding.io.exe_pipe_regWrite_out := EXE_MEM.io.regWrite_out
  forwarding.io.exe_pipe_rd_out := EXE_MEM.io.rd_out
  forwarding.io.mem_pipe_regWrite_out := MEM_WB.io.regWrite_out
  forwarding.io.mem_pipe_rd_out := MEM_WB.io.rd_out
  forwarding.io.id_pipe_rs1_sel_out:= ID_EXE.io.rs1_sel_Out
  forwarding.io.id_pipe_rs2_sel_out := ID_EXE.io.rs2_sel_Out


  when(ID_EXE.io.opr_A_sel_Out === "b10".U){
    alu.io.oper_a := ID_EXE.io.operandA_out
  }.otherwise{
    when(forwarding.io.alu_A === "b00".U){
      alu.io.oper_a := ID_EXE.io.operandA_out
    }.elsewhen(forwarding.io.alu_A === "b01".U){
      alu.io.oper_a := EXE_MEM.io.alu_Output_output
    }.elsewhen(forwarding.io.alu_A === "b10".U){
      alu.io.oper_a := reg.io.writeData
    }.otherwise{
      alu.io.oper_a := ID_EXE.io.operandA_out
    }
  }

  when(ID_EXE.io.opr_B_sel_Out === "b1".U){
    alu.io.oper_b := ID_EXE.io.operandB_out
    when(forwarding.io.alu_B === "b00".U) {
      EXE_MEM.io.rs2_in := ID_EXE.io.operandB_out
    }.elsewhen(forwarding.io.alu_B === "b01".U) {
      EXE_MEM.io.rs2_in := EXE_MEM.io.alu_Output_output
    }.elsewhen(forwarding.io.alu_B === "b10".U) {
      EXE_MEM.io.rs2_in := reg.io.writeData
    }.otherwise {
      EXE_MEM.io.rs2_in := ID_EXE.io.operandB_out
    }
  }.otherwise{
    when(forwarding.io.alu_B === "b00".U) {
      alu.io.oper_b := ID_EXE.io.operandB_out
      EXE_MEM.io.rs2_in := ID_EXE.io.operandB_out
    }.elsewhen(forwarding.io.alu_B === "b01".U) {
      alu.io.oper_b := EXE_MEM.io.alu_Output_output
      EXE_MEM.io.rs2_in := EXE_MEM.io.alu_Output_output
    }.elsewhen(forwarding.io.alu_B === "b10".U) {
      alu.io.oper_b := reg.io.writeData
      EXE_MEM.io.rs2_in := reg.io.writeData
    }.otherwise {
      alu.io.oper_b := ID_EXE.io.operandB_out
      EXE_MEM.io.rs2_in := ID_EXE.io.operandB_out
    }
  }

//branch forwarding

	// FOR REGISTER RS1 in BRANCH FORWARDING

	branchForward.io.ID_rd := ID_EXE.io.rd_out
	branchForward.io.ID_MemRead := ID_EXE.io.memRead_out
	branchForward.io.EX_rd := EXE_MEM.io.rd_out
	branchForward.io.EX_MemRead := EXE_MEM.io.memRead_out
	branchForward.io.MEM_rd := MEM_WB.io.rd_out
	branchForward.io.MEM_MemRead := MEM_WB.io.memRead_out
	branchForward.io.rs1_sel := IF_ID.io.ins_out(19,15)
  branchForward.io.rs2_sel := IF_ID.io.ins_out(24,20)
	branchForward.io.control_branch := control.io.out_branch

	when(branchForward.io.forward_a === "b0000".U) {
      
      		branch.io.rs1 := reg.io.rs1
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0001".U) {
      
      		branch.io.rs1 := alu.io.out
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0010".U) {
      
      		branch.io.rs1 := EXE_MEM.io.alu_Output_output
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0011".U) {
      
      		branch.io.rs1 := reg.io.writeData
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0100".U) {
     
      		branch.io.rs1 := dataMem.io.memOut
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0101".U) {
   
      		branch.io.rs1 := reg.io.writeData
      		jalr.io.rs1 := reg.io.rs1
    	}.elsewhen(branchForward.io.forward_a === "b0110".U) {
        
        	jalr.io.rs1 := alu.io.out
        	branch.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0111".U) {
        
        	jalr.io.rs1 := EXE_MEM.io.alu_Output_output
        	branch.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b1000".U) {
        	jalr.io.rs1 := reg.io.writeData
        	branch.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b1001".U) {
        
        	jalr.io.rs1 := dataMem.io.memOut
        	branch.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b1010".U) {
        	jalr.io.rs1 := reg.io.writeData
        	branch.io.rs1 := reg.io.rs1
    	}
      	.otherwise {
        	branch.io.rs1 := reg.io.rs1
        	jalr.io.rs1 := reg.io.rs1
    	}


	// FOR REGISTER RS2 in BRANCH FORWARDING
    	when(branchForward.io.forward_a === "b0000".U) {
      		branch.io.rs2 := reg.io.rs2
    	} .elsewhen(branchForward.io.forward_a === "b0001".U) {
      		branch.io.rs2 := alu.io.out
    	} .elsewhen(branchForward.io.forward_a === "b0010".U) {
      		branch.io.rs2 := EXE_MEM.io.alu_Output_output
    	} .elsewhen(branchForward.io.forward_a === "b0011".U) {
      		branch.io.rs2 := reg.io.writeData
    	} .elsewhen(branchForward.io.forward_a === "b0100".U) {
      		branch.io.rs2 := dataMem.io.memOut
    	} .elsewhen(branchForward.io.forward_a === "b0101".U) {
      		branch.io.rs2 := reg.io.writeData
    	}.otherwise{
        	branch.io.rs2 := reg.io.rs2
      	}

    	jalr.io.imm:= imm.io.i_imm


	//structural hazard
	when(branchForward.io.forward_b === "b00000".U){
		branch.io.rs2 := reg.io.rs2
	}.elsewhen(branchForward.io.forward_b === "b00001".U){
		branch.io.rs2 := alu.io.out
	}.elsewhen(branchForward.io.forward_b === "b00010".U){
		branch.io.rs2 := EXE_MEM.io.alu_Output_output
	}.elsewhen(branchForward.io.forward_b === "b00011".U){
		branch.io.rs2 := reg.io.writeData
	}.otherwise{
		branch.io.rs2 := reg.io.rs2
	}}