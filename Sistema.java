// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// Fase 3 - máquina virtual (vide enunciado correspondente)
//Teste de commit

import java.util.*;
public class Sistema {
	
	// -------------------------------------------------------------------------------------------------------
	// --------------------- H A R D W A R E - definicoes de HW ---------------------------------------------- 

	// -------------------------------------------------------------------------------------------------------
	// --------------------- M E M O R I A -  definicoes de opcode e palavra de memoria ---------------------- 
	
	public class Word { 	// cada posicao da memoria tem uma instrucao (ou um dado)
		public Opcode opc; 	//
		public int r1; 		// indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
		public int r2; 		// indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
		public int p; 		// parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

		public Word(Opcode _opc, int _r1, int _r2, int _p) {  
			opc = _opc;   r1 = _r1;    r2 = _r2;	p = _p;
		}
	}
    // -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
    // --------------------- C P U  -  definicoes da CPU ----------------------------------------------------- 

	public enum Opcode {
		DATA, ___,		    // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE,  JMPIM, JMPIGM, JMPILM, JMPIEM, STOP, JMPGE,   // desvios e parada
		ADDI, SUBI,  ADD, SUB, MULT,         // matematicos
		LDI, LDD, STD,LDX, STX, SWAP,        // movimentacao
		TRAP;
	}

	public enum interrupt{ //interrupcoes da CPU
		None, Overflow, InvalidOpcode, InvalidAdrress, Stop;
	}

	public class CPU {
							// característica do processador: contexto da CPU ...
		private int pc; 			// ... composto de program counter,
		private Word ir; 			// instruction register,
		private int[] reg;       	// registradores da CPU
		
		interrupt interrupcaoAtiva;
		//private boolean[] interrup = new boolean[5]; // {__,1-Overflow,2-}

		private Word[] m;   // CPU acessa MEMORIA, guarda referencia 'm' a ela. memoria nao muda. ee sempre a mesma.
			
		public CPU(Word[] _m) {     // ref a MEMORIA e interrupt handler passada na criacao da CPU
			m = _m; 				// usa o atributo 'm' para acessar a memoria.
			reg = new int[10]; 		// aloca o espaço dos registradores
		}

		public void setContext(int _pc) {  // no futuro esta funcao vai ter que ser 
			pc = _pc;                                              // limite e pc (deve ser zero nesta versao)
		}
	
		private void dump(Word w) {
			System.out.print("[ "); 
			System.out.print(w.opc); System.out.print(", ");
			System.out.print(w.r1);  System.out.print(", ");
			System.out.print(w.r2);  System.out.print(", ");
			System.out.print(w.p);  System.out.println("  ] ");
		}

        private void showState(){
			 System.out.println("       "+ pc); 
			   System.out.print("           ");
			 for (int i=0; i<reg.length; i++) { System.out.print("r"+i);   System.out.print(": "+reg[i]+"     "); };  
			 System.out.println("");
			 System.out.print("           ");  dump(ir);
		}


		private void resetInterrupt() {
			interrupcaoAtiva = interrupt.None;
		}


		public void run() { 		// execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente setado
			boolean stateRun = true;
			while (stateRun) { 			// ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
				try{
					// FETCH
						ir = m[pc]; 	// busca posicao da memoria apontada por pc, guarda em ir
						//if debug
							showState();
					// EXECUTA INSTRUCAO NO ir
						switch (ir.opc) { // para cada opcode, sua execução



						case STD: // [A] <- Rs
							    m[ir.p].opc = Opcode.DATA;
							    m[ir.p].p = reg[ir.r1];
							    pc++;
						break;

            			case LDI: // Rd <- k
								reg[ir.r1] = ir.p;
								pc++;
								break;


							


							case LDD: // Rd <- [A] //Here
								//m[ir.p].opc = Opcode.DATA; //Leitura não precisa saber se é um dado
								reg[ir.r1] = m[ir.p].p; //Ajustar para memoria
								pc++;
								break;
							
							case LDX: //Rd <- [Rs]   // Here
								//m[reg[ir.r2]].opc = Opcode.DATA;   //Leitura não precisa saber o que é feito    
								reg[ir.r1] = reg[ir.r2];  //Depois ver com o professor
								pc++;
								break;


							case STX: // [Rd] ←Rs
								m[reg[ir.r1]].opc = Opcode.DATA;      
								m[reg[ir.r1]].p = reg[ir.r2];          
								pc++;
							break;

							


               case ADD: // Rd ← Rd + Rs
								reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
								pc++;
								break;
					



							case MULT: // Rd ← Rd * Rs
								
								try {
									long tmp = (long) reg[ir.r1] * (long) reg[ir.r2];
									if(tmp >= Integer.MAX_VALUE){
										throw new Exception("Erro de multiplicacao");
									}else{
										reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
									}
									pc++;
								} catch (Exception e) {
									// gera um overflow
									// -->  LIGA INT  (1)
									interrupcaoAtiva = interrupt.Overflow;
								}
								
								break;

            	case ADDI: // Rd ← Rd + k

								reg[ir.r1] = reg[ir.r1] + ir.p;
								pc++;
								break;

						case SUB: // Rd <- Rd - Rs

							reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
							pc++;
							break;											 
								
         		case SUBI: //Here
								reg[ir.r1] = reg[ir.r1] - ir.p;
								pc++;
							break;				

							case JMPI: // Here
									pc = ir.r1;
							break;

							case JMP: //  PC ← k
									pc = ir.p;
								break;
								
							case JMPIL: //HEre
								if(reg[ir.r2] < 0){
									pc = reg[ir.r1];
								} else{
									pc++;
								}							
								break;
							
							case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1

								if (reg[ir.r2] > 0) {
									pc = reg[ir.r1];
								} else {
									pc++;
								}
								break;


							case JMPIE: // If Rc = 0 Then PC ← Rs Else PC ← PC +1

								if (reg[ir.r2] == 0) {
									pc = reg[ir.r1];
								} else {
									pc++;
								}
								break;

							
							case JMPIM://Here
								pc =  m[ir.p].p;


						case JMPIGM: //if Rc > 0 then PC <- [A] Else PC <- PC +1 // Here
						if (reg[ir.r2] > 0) {
							pc = m[ir.p].p;
						} else {
							pc++;
						}
						break;

						case JMPILM: // if Rc < 0 then PC <- [A]		Else PC <- PC +1 //Here
						if(reg[ir.r2] < 0){
							pc = m[ir.p].p;
						} else{
							pc++;
						}							
						break;

						case JMPIEM: // if Rc = 0 then PC <- [A]  Else PC <- PC +1
						if (reg[ir.r2] == 0) {
							pc = m[ir.p].p;
						} else {
							pc++;
						}
						break;

						case JMPGE: //Jump Greater Equal Adicionar no pdf
						if (reg[ir.r2] >= 0) {
							pc = reg[ir.r1];
						} else {
							pc++;
						}
						break;

							

							case TRAP:

								if (reg[8] == 1){ //Verificado o valor dentro do registrador 8 || TRAP = 1 -> chamada de IN
									Scanner myObj = new Scanner(System.in); // instancia leituras do java
									System.out.print("Input integer: ");
									String inputUser = myObj.nextLine(); //le o numero do usuario
									m[reg[9]].p = Integer.parseInt(inputUser); // conforme a entrada e salva na posição da memoria 
									m[reg[9]].opc = Opcode.DATA;
									//Conforme exemplo do professor
									//|| reg[9] (obtem o valor dentro do registrador) =4, entao, m[4], logo m[4] <- input 
								}

								if (reg[8] == 2){ //TRAP = 2 -> chamada de OUT
									int output = m[reg[9]].p; //reg[9]=10, logo, m[10] || output <- m[10]
									System.out.println(output);
									//?? forma flexíveL, verificar ultima especificacao da Fase3
								}
								pc++;								
								break;

							case STOP: // por enquanto, para execucao
								interrupcaoAtiva = interrupt.Stop;
								break;
							default:
							    // opcode desconhecido
								// liga interrup (2)
								interrupcaoAtiva = interrupt.InvalidOpcode;
					}
				}
				catch(ArrayIndexOutOfBoundsException e){ // execoes para acesso a elemento de memoria maior que o vetor
					interrupcaoAtiva = interrupt.InvalidAdrress;
				}
				// VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
				 // if int ligada - vai para tratamento da int 
				//     desviar para rotina java que trata int
				if(interrupcaoAtiva!=interrupt.None){
					trataTnterrupcoes(interrupcaoAtiva); //trata interrupcao
					stateRun=false; // para execucao do loop while/programa
				}
				
			}
		}
	}
    // ------------------ C P U - fim ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	
    // ------------------- V M  - constituida de CPU e MEMORIA -----------------------------------------------
    // -------------------------- atributos e construcao da VM -----------------------------------------------
	public class VM {//Mudar aqui para implementar o Gerenciador de Memoria 
		public int tamMem, tamPag, tamFrame, nroFrames;   
        public Word[] m;     
        public CPU cpu;	
	

        /*public VM(){    
	     // memória
  		 	 tamMem = 1024;
			 m = new Word[tamMem]; // m ee a memoria
			 for (int i=0; i<tamMem; i++) { m[i] = new Word(Opcode.___,-1,-1,-1); };
	  	 // cpu
			 cpu = new CPU(m);   // cpu acessa memória
	    }*/
		
		public VM(){ //Implementação da GM
			//memória
			tamMem = 1024;
			tamPag = 16;
			tamFrame = tamPag;
			nroFrames = tamMem / tamPag;
			int[]  mem = new int[tamMem];
			//cpu
			cpu = new CPU(m);
			
		}
	}
    // ------------------- V M  - fim ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

    // --------------------H A R D W A R E - fim -------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// ------------------- S O F T W A R E - inicio ----------------------------------------------------------

		// -------------------------------------------  funcoes de um monitor
	public class Monitor {
			public void dump(Word w) {
				System.out.print("[ "); 
				System.out.print(w.opc); System.out.print(", ");
				System.out.print(w.r1);  System.out.print(", ");
				System.out.print(w.r2);  System.out.print(", ");
				System.out.print(w.p);  System.out.println("  ] ");
			}
			public void dump(Word[] m, int ini, int fim) {
				for (int i = ini; i < fim; i++) {
					System.out.print(i); System.out.print(":  ");  dump(m[i]);
				}
			}
			public void carga(Word[] p, Word[] m) {    // significa ler "p" de memoria secundaria e colocar na principal "m"
				for (int i = 0; i < p.length; i++) {
					m[i].opc = p[i].opc;     m[i].r1 = p[i].r1;     m[i].r2 = p[i].r2;     m[i].p = p[i].p;
				}
			}
			public void executa() {      
				vm.cpu.resetInterrupt(); 		//zera os interruptores
				vm.cpu.setContext(0);          // monitor seta contexto - pc aponta para inicio do programa 
				vm.cpu.run();                  //                         e cpu executa
				                               // note aqui que o monitor espera que o programa carregado acabe normalmente
											   // nao ha protecoes...  o que poderia acontecer ?
				}
		}
	   // -------------------------------------------  
		



	// -------------------------------------------------------------------------------------------------------
    // -------------------  S I S T E M A --------------------------------------------------------------------

	public VM vm;
	public Monitor monitor;
	public static Programas progs;

    public Sistema(){   // a VM com tratamento de interrupções
		 vm = new VM();
		 monitor = new Monitor();
		 progs = new Programas(); 
	}

	public void roda(Word[] programa){
			monitor.carga(programa, vm.m);    
			System.out.println("---------------------------------- programa carregado ");
			monitor.dump(vm.m, 0, 90); //Muda o total do Dump
			monitor.executa();        
			System.out.println("---------------------------------- após execucao ");
			monitor.dump(vm.m, 0, 90); //Muda o total do Dump
		}

	public void trataTnterrupcoes(interrupt i){
		System.out.println("I-N-T-E-R-R-U-P-T-I-O-N");
		if(i==interrupt.InvalidAdrress) System.out.println("Acesso invalido a memoria"); 
		if(i==interrupt.InvalidOpcode) System.out.println("Opcode invalido");
		if(i==interrupt.Overflow) System.out.println("OverFlow");
		if(i==interrupt.Stop) System.out.println("Fim da execucao do programa");   
	}

    // -------------------  S I S T E M A - fim --------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // ------------------- instancia e testa sistema
	public static void main(String args[]) {
		Sistema s = new Sistema();

	    //s.roda(progs.fibonacci10);           // "progs" significa acesso/referencia ao programa em memoria secundaria
		// s.roda(progs.progMinimo);
		// s.roda(progs.fatorial);

		//s.roda(progs.PB);
		//s.roda(progs.testOverFlow);
		//s.roda(progs.testInvalidOpcode);
		//s.roda(progs.testInvalidAdrress);
		//s.roda(progs.testIN);
		//s.roda(progs.testOUT);
		//s.roda(progs.PB);
		s.roda(progs.PA);
		//s.roda(progs.PC);

	}	

    // -------------------------------------------------------------------------------------------------------
    // --------------- TUDO ABAIXO DE MAIN É AUXILIAR PARA FUNCIONAMENTO DO SISTEMA - nao faz parte 

   //  -------------------------------------------- programas aa disposicao para copiar na memoria (vide carga)
   public class Programas {
	  /* public Word[] progMinimo = new Word[] {
		    //       OPCODE      R1  R2  P         :: VEJA AS COLUNAS VERMELHAS DA TABELA DE DEFINICAO DE OPERACOES
			//                                     :: -1 SIGNIFICA QUE O PARAMETRO NAO EXISTE PARA A OPERACAO DEFINIDA
		    new Word(Opcode.LDI, 0, -1, 999), 		
			new Word(Opcode.STD, 0, -1, 10), 
			new Word(Opcode.STD, 0, -1, 11), 
			new Word(Opcode.STD, 0, -1, 12), 
			new Word(Opcode.STD, 0, -1, 13), 
			new Word(Opcode.STD, 0, -1, 14), 
			new Word(Opcode.STOP, -1, -1, -1) };

	   public Word[] fibonacci10 = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
			new Word(Opcode.LDI, 1, -1, 0), 
			new Word(Opcode.STD, 1, -1, 20),    // 20 posicao de memoria onde inicia a serie de fibonacci gerada  
			new Word(Opcode.LDI, 2, -1, 1),
			new Word(Opcode.STD, 2, -1, 21),      
			new Word(Opcode.LDI, 0, -1, 22),       
			new Word(Opcode.LDI, 6, -1, 6),
			new Word(Opcode.LDI, 7, -1, 30),       
			new Word(Opcode.LDI, 3, -1, 0), 
			new Word(Opcode.ADD, 3, 1, -1),
			new Word(Opcode.LDI, 1, -1, 0), 
			new Word(Opcode.ADD, 1, 2, -1), 
			new Word(Opcode.ADD, 2, 3, -1),
			new Word(Opcode.STX, 0, 2, -1), 
			new Word(Opcode.ADDI, 0, -1, 1), 
			new Word(Opcode.SUB, 7, 0, -1),
			new Word(Opcode.JMPIG, 6, 7, -1), 
			new Word(Opcode.STOP, -1, -1, -1),   // POS 16
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),   // POS 20
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1)  // ate aqui - serie de fibonacci ficara armazenada
			   };   

	   public Word[] fatorial = new Word[] { 	 // este fatorial so aceita valores positivos.   nao pode ser zero
												 // linha   coment
			new Word(Opcode.LDI, 0, -1, 6),      // 0   	r0 é valor a calcular fatorial
			new Word(Opcode.LDI, 1, -1, 1),      // 1   	r1 é 1 para multiplicar (por r0)
			new Word(Opcode.LDI, 6, -1, 1),      // 2   	r6 é 1 para ser o decremento
			new Word(Opcode.LDI, 7, -1, 8),      // 3   	r7 tem posicao de stop do programa = 8
			new Word(Opcode.JMPIE, 7, 0, 0),     // 4   	se r0=0 pula para r7(=8)
			new Word(Opcode.MULT, 1, 0, -1),     // 5   	r1 = r1 * r0
			new Word(Opcode.SUB, 0, 6, -1),      // 6   	decrementa r0 1 
			new Word(Opcode.JMP, -1, -1, 4),     // 7   	vai p posicao 4
			new Word(Opcode.STD, 1, -1, 10),     // 8   	coloca valor de r1 na posição 10
			new Word(Opcode.STOP, -1, -1, -1),    // 9   	stop

			new Word(Opcode.DATA, -1, -1, -1) };  // 10   ao final o valor do fatorial estará na posição 10 da memória*/

		public Word[] PB = new Word[]{

			new Word(Opcode.LDI, 0, -1, 4),      // 0   	Valor armazenado na memoria
			new Word(Opcode.LDI, 1, -1, 13),     // 1   	Linha do salto do jump de 0
			new Word(Opcode.LDI, 2, -1, 7),      // 2   	Linha do salto do jump do loop
			new Word(Opcode.LDI, 3, -1, 1),      // 3   	Valor do inicio do Fatoral
			new Word(Opcode.LDI, 4, -1, -1),     // 4   	Valor caso seja negativo
			new Word(Opcode.LDI, 5, -1, 11),     // 5 
			//Teste do Zero
			new Word(Opcode.JMPIL, 1, 0, -1),    // 6   	Se R0 for menor que 0 salta pro fim
			//LOOP do Fatorial
			new Word(Opcode.JMPIE,5, 0, -1),	 //7
			new Word(Opcode.MULT, 3, 0, -1),     // 8   	Multiplico o valor dele por ele mesmo
			new Word(Opcode.SUBI, 0, -1, 1),     // 9   	Diminuo o valor do numero fatoral
			new Word(Opcode.JMPGE, 2, 0, -1),    // 10   	coloca valor de r1 na posição 10
			//FIM do progama para o Fatorial
			new Word(Opcode.STD, 3, -1, 20),     // 11   	Armazeno na memoria o resultado do faotrial
			new Word(Opcode.STOP, -1, -1, -1),  // 12	  	Termina o progama
			//FIM do programa para o Negativo
			new Word(Opcode.STD, 4, -1, 20),     // 13   	Salvo -1 no inicio da memoria	
      new Word(Opcode.STOP, -1, -1, -1) }; // 14   	Termina o progama	
     
			
			
		public Word[] testOverFlow = new Word[]{
			new Word(Opcode.LDI, 0, -1, 2147483647),      // 0   	Valor armazenado na memoria
			new Word(Opcode.LDI, 1, -1, 2147483647),     // 1   	Valor armazenado na memoria
			new Word(Opcode.MULT, 0, 1, -1)      // 2   	aplica multiplicacao (forcando overflow)
		};

		public Word[] testInvalidOpcode = new Word[]{
			new Word(Opcode.DATA, -1, -1, -1),      // 0   	DATA nao e opcode de CPU
		};

		public Word[] testInvalidAdrress = new Word[]{
			new Word(Opcode.STD, 0, -1, 2048),      // 0   	2048 Endereco invalido (Ex de max 1024)
		};

		public Word[] testIN = new Word[]{
			new Word(Opcode.LDI, 8, -1, 1),     	 
			new Word(Opcode.LDI, 9, -1, 4),     	 
			new Word(Opcode.TRAP, -1, -1, -1),      
			new Word(Opcode.STOP, -1, -1, -1),      
		};
		public Word[] testOUT = new Word[]{
			new Word(Opcode.LDI, 0, -1, 12345),      
			new Word(Opcode.STD, 0, -1, 10),     	 
			new Word(Opcode.LDI, 8, -1, 2),     	 
			new Word(Opcode.LDI, 9, -1, 10),     	 
			new Word(Opcode.TRAP, -1, -1, -1), 
			new Word(Opcode.STOP, -1, -1, -1),      
		}; 


			                                
	
	
		public Word[] PA = new Word[]{

		new Word(Opcode.LDI, 0, -1, 4),
		new Word(Opcode.LDI, 1, -1, 15), 
		new Word(Opcode.LDI, 2, -1, -1), 
		new Word(Opcode.LDI, 3, -1, 14), 
		new Word(Opcode.LDI, 4, -1, 1), 
		new Word(Opcode.LDI, 5, -1, 20),
		//JMP para testar o zero
		new Word(Opcode.JMPIL, 1, 0, -1),
		//Loop Fibonacci
		new Word(Opcode.JMPIE, 3, 0, -1),
		new Word(Opcode.STX, 5, 4, -1),
		new Word(Opcode.ADDI, 5, -1, 1),
		new Word(Opcode.LDX, 6, 4, -1),
		new Word(Opcode.ADD, 4, 6, -1),
		new Word(Opcode.SUBI, 0, -1, 1),
		new Word(Opcode.JMP, -1, -1, 7),
		//Fim do programa caso tenha tido sequencia
		new Word(Opcode.STOP, -1, -1, -1),
		//Fim do programa caso tenha sido um zero
		new Word(Opcode.STD, 2, -1, 20),
		new Word(Opcode.STOP, -1, -1, -1)                         
		};

		public Word[] PC =new Word[]{
			new Word(Opcode.LDI, 0, -1, 31),
			new Word(Opcode.LDI, 1, -1, 32),
			new Word(Opcode.LDI, 2, -1, 40),
			new Word(Opcode.LDI, 3, -1, 41),
			new Word(Opcode.LDI, 4, -1, 48),
			//coloca elementos no vetor
			new Word(Opcode.LDI, 4, -1, 8),
			new Word(Opcode.STD, 4, -1, 40),
			new Word(Opcode.LDI, 4, -1, 7),
			new Word(Opcode.STD, 4, -1, 41),
			new Word(Opcode.LDI, 4, -1, 6),
			new Word(Opcode.STD, 4, -1, 42),
			new Word(Opcode.LDI, 4, -1, 5),
			new Word(Opcode.STD, 4, -1, 43),
			new Word(Opcode.LDI, 4, -1, 4),
			new Word(Opcode.STD, 4, -1, 44),
			new Word(Opcode.LDI, 4, -1, 3),
			new Word(Opcode.STD, 4, -1, 45),
			new Word(Opcode.LDI, 4, -1, 2),
			new Word(Opcode.STD, 4, -1, 46),
			new Word(Opcode.LDI, 4, -1, 1),
			new Word(Opcode.STD, 4, -1, 47),
			//Faz o bubble sort do Algoritmo
			new Word(Opcode.JMPIL, 6, 0, -1),
			new Word(Opcode.LDX, 5, -1, 2),
			new Word(Opcode.LDX, 6, -1, 3),
			new Word(Opcode.STD, 6, -1, 48),
			new Word(Opcode.SUB, 5, 5, -1),
			new Word(Opcode.JMPIL, 6, 1, -1),
			new Word(Opcode.LDX, 6, -1, 7),
			new Word(Opcode.ADDI, 2, -1, 1),
			new Word(Opcode.ADDI, 3, -1, 1),
			new Word(Opcode.JMP, -1, -1, 21),
			//Para o progama
			new Word(Opcode.STOP, -1, -1, -1),
			//Faz o swap
			new Word(Opcode.LDX, 6, -1, 7),
			new Word(Opcode.STX, 2, 6, -1),
			new Word(Opcode.STX, 3, 5, -1),
			new Word(Opcode.ADDI, 2, -1, 1),
			new Word(Opcode.ADDI, 3, -1, 1),
			new Word(Opcode.JMP, -1, -1, 21),
			//Stop caso de problema no jump
			new Word(Opcode.STOP, -1, -1, -1)
		};
		
	}
}



