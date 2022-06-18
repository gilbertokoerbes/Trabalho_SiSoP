// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// Fase 3 - máquina virtual (vide enunciado correspondente)
//Teste de commit

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Sistema {

	// -------------------------------------------------------------------------------------------------------
	// --------------------- H A R D W A R E - definicoes de HW
	// ----------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// --------------------- M E M O R I A - definicoes de opcode e palavra de
	// memoria ----------------------

	public class Word { // cada posicao da memoria tem uma instrucao (ou um dado)
		public Opcode opc; //
		public int r1; // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
		public int r2; // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
		public int p; // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

		public Word(Opcode _opc, int _r1, int _r2, int _p) {
			opc = _opc;
			r1 = _r1;
			r2 = _r2;
			p = _p;
		}
	}
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// --------------------- C P U - definicoes da CPU
	// -----------------------------------------------------

	public enum Opcode {
		DATA, ___, // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, STOP, JMPGE, // desvios e parada
		ADDI, SUBI, ADD, SUB, MULT, // matematicos
		LDI, LDD, STD, LDX, STX, SWAP, // movimentacao
		TRAP;
	}

	public enum interrupt { // interrupcoes da CPU
		None, Overflow, InvalidOpcode, InvalidAdrress, Stop, Timer;
	}

	public class CPU {
		// característica do processador: contexto da CPU ...
		private int pc; // ... composto de program counter,
		private Word ir; // instruction register,
		private int[] reg; // registradores da CPU

		interrupt interrupcaoAtiva; // interrupcao a ser guardada pelo processador;
		int DeltaTimer; // Varial para simultar o Timer para interrupção TIMER no processado

		private Word[] m; // CPU acessa MEMORIA, guarda referencia 'm' a ela. memoria nao muda. ee sempre
							// a mesma.
		GM.tabelaPaginaProcesso pagesProcess;
		STATE state;

		// =//=//=//=//=//=//=//=//=//=//=//=//=//=//=//=//=

		public CPU(Word[] _m) { // ref a MEMORIA e interrupt handler passada na criacao da CPU
			m = _m; // usa o atributo 'm' para acessar a memoria.
			reg = new int[10]; // aloca o espaço dos registradores
		}

		public void setContext(int _pc, GM.tabelaPaginaProcesso pagesProcess, STATE state) { // no futuro esta funcao
																								// vai ter que ser
			// /// incremenado tabelaPagina do
			// processo para o contexto
			pc = _pc; // limite e pc (deve ser zero nesta versao)
			this.pagesProcess = pagesProcess;
			this.state = state;
		}

		private void dump(Word w) {
			System.out.print("[ ");
			System.out.print(w.opc);
			System.out.print(", ");
			System.out.print(w.r1);
			System.out.print(", ");
			System.out.print(w.r2);
			System.out.print(", ");
			System.out.print(w.p);
			System.out.println("  ] ");
		}

		private void showState() {
			System.out.println("       " + pc);
			System.out.print("           ");
			for (int i = 0; i < reg.length; i++) {
				System.out.print("r" + i);
				System.out.print(": " + reg[i] + "     ");
			}
			;
			System.out.println("");
			System.out.print("           ");
			dump(ir);
		}

		private void resetInterrupt() {
			interrupcaoAtiva = interrupt.None;
			DeltaTimer = 5;
		}

		public void run() { // execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente
							// setado
			boolean stateRun = true;
			while (stateRun) { // ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
				try {
					// FETCH
					int addressT = vm.gm.translate(pc, pagesProcess);
					int addressT_2; // usado como reserva em caso especifico
					ir = m[addressT]; // busca posicao da memoria apontada por pc, guarda em ir
					// if debug
					showState();
					// EXECUTA INSTRUCAO NO ir
					switch (ir.opc) { // para cada opcode, sua execução

						case STD: // [A] <- Rs
							addressT = vm.gm.translate(ir.p, pagesProcess); // address assume o valor traduzido de ir.p
							m[addressT].opc = Opcode.DATA;
							m[addressT].p = reg[ir.r1];
							pc++;
							break;

						case LDI: // Rd <- k
							reg[ir.r1] = ir.p;
							pc++;
							break;

						case LDD: // Rd <- [A] //Here
							// m[ir.p].opc = Opcode.DATA; //Leitura não precisa saber se é um dado
							addressT = vm.gm.translate(ir.r1, pagesProcess); // address assume o valor traduzido de
																				// ir.r1
							addressT_2 = vm.gm.translate(ir.p, pagesProcess);
							reg[addressT] = m[addressT_2].p; // Ajustar para memoria
							pc++;
							break;

						case LDX: // Rd <- [Rs] // Here
							// m[reg[ir.r2]].opc = Opcode.DATA; //Leitura não precisa saber o que é feito
							reg[ir.r1] = m[reg[ir.r2]].p; // Corrigido para pegar os dados internos
							pc++;
							break;

						case STX: // [Rd] <- Rs
							addressT = vm.gm.translate(reg[ir.r1], pagesProcess);
							m[addressT].opc = Opcode.DATA;
							m[addressT].p = reg[ir.r2];
							pc++;
							break;

						case ADD: // Rd <- Rd + Rs
							reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
							pc++;
							break;

						case MULT: // Rd <- Rd * Rs

							try {
								long tmp = (long) reg[ir.r1] * (long) reg[ir.r2];
								if (tmp >= Integer.MAX_VALUE) {
									throw new Exception("Erro de multiplicacao");
								} else {
									reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
								}
								pc++;
							} catch (Exception e) {
								// gera um overflow
								// --> LIGA INT (1)
								interrupcaoAtiva = interrupt.Overflow;
							}

							break;

						case ADDI: // Rd <- Rd + k

							reg[ir.r1] = reg[ir.r1] + ir.p;
							pc++;
							break;

						case SUB: // Rd <- Rd - Rs

							reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
							pc++;
							break;

						case SUBI: // Here
							reg[ir.r1] = reg[ir.r1] - ir.p;
							pc++;
							break;

						case JMPI: // Here
							pc = ir.r1;
							break;

						case JMP: // PC <- k
							pc = ir.p;
							break;

						case JMPIL: // HEre
							if (reg[ir.r2] < 0) {
								pc = reg[ir.r1];
							} else {
								pc++;
							}
							break;

						case JMPIG: // If Rc > 0 Then PC <- Rs Else PC <- PC +1

							if (reg[ir.r2] > 0) {
								pc = reg[ir.r1];
							} else {
								pc++;
							}
							break;

						case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1

							if (reg[ir.r2] == 0) {
								pc = reg[ir.r1];
							} else {
								pc++;
							}
							break;

						case JMPIM:// Here
							addressT = vm.gm.translate(ir.p, pagesProcess);
							pc = m[addressT].p;

						case JMPIGM: // if Rc > 0 then PC <- [A] Else PC <- PC +1 // Here
							if (reg[ir.r2] > 0) {
								addressT = vm.gm.translate(ir.p, pagesProcess);
								pc = m[addressT].p;
							} else {
								pc++;
							}
							break;

						case JMPILM: // if Rc < 0 then PC <- [A] Else PC <- PC +1 //Here
							if (reg[ir.r2] < 0) {
								addressT = vm.gm.translate(ir.p, pagesProcess);
								pc = m[addressT].p;
							} else {
								pc++;
							}
							break;

						case JMPIEM: // if Rc = 0 then PC <- [A] Else PC <- PC +1
							if (reg[ir.r2] == 0) {
								addressT = vm.gm.translate(ir.p, pagesProcess);
								pc = m[addressT].p;
							} else {
								pc++;
							}
							break;

						case JMPGE: // Jump Greater Equal Adicionar no pdf
							if (reg[ir.r2] >= 0) {
								pc = reg[ir.r1];
							} else {
								pc++;
							}
							break;

						case TRAP:

							if (reg[8] == 1) { // Verificado o valor dentro do registrador 8 || TRAP = 1 -> chamada de
												// IN
								Scanner myObj = new Scanner(System.in); // instancia leituras do java
								System.out.print("Input integer: ");
								String inputUser = myObj.nextLine(); // le o numero do usuario

								addressT = vm.gm.translate(reg[9], pagesProcess);
								m[addressT].p = Integer.parseInt(inputUser); // conforme a entrada e salva na posição da
																				// memoria
								m[addressT].opc = Opcode.DATA;
								// Conforme exemplo do professor
								// || reg[9] (obtem o valor dentro do registrador) =4, entao, m[4], logo m[4] <-
								// input
							}

							if (reg[8] == 2) { // TRAP = 2 -> chamada de OUT
								addressT = vm.gm.translate(reg[9], pagesProcess);
								int output = m[addressT].p; // reg[9]=10, logo, m[10] || output <- m[10]
								System.out.println(output);
								// ?? forma flexíveL, verificar ultima especificacao da Fase3
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
				} catch (IndexOutOfBoundsException e) { // execoes para acesso a elemento de memoria maior que o
														// vetor
					interrupcaoAtiva = interrupt.InvalidAdrress;
				}
				// Decremento do timer
				DeltaTimer--;
				if (DeltaTimer == 0)
					interrupcaoAtiva = interrupcaoAtiva.Timer;
				// VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
				// if int ligada - vai para tratamento da int
				// desviar para rotina java que trata int
				if (interrupcaoAtiva != interrupt.None) {
					trataTnterrupcoes(interrupcaoAtiva); // trata interrupcao
					stateRun = false; // para execucao do loop while/programa
				}

			}
		}
	}
	// ------------------ C P U - fim
	// ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// ------------------- V M - constituida de CPU e MEMORIA
	// -----------------------------------------------
	// -------------------------- atributos e construcao da VM
	// -----------------------------------------------
	public class VM {// Mudar aqui para implementar o Gerenciador de Memoria
		public int tamMem, tamPag, tamFrame, nroFrames;
		public GM gm;
		public Word[] m;
		public CPU cpu;

		/*
		 * public VM(){
		 * // memória
		 * tamMem = 1024;
		 * m = new Word[tamMem]; // m ee a memoria
		 * for (int i=0; i<tamMem; i++) { m[i] = new Word(Opcode.___,-1,-1,-1); };
		 * // cpu
		 * cpu = new CPU(m); // cpu acessa memória
		 * }
		 */

		public VM() {
			// memória
			tamMem = 1024;
			m = new Word[tamMem]; // m ee a memoria
			for (int i = 0; i < tamMem; i++) {
				m[i] = new Word(Opcode.___, -1, -1, -1);
			}

			// paginação
			// tamFrame = tamPag = 16;
			tamFrame = tamPag = 16;/// test
			nroFrames = tamMem / tamFrame;

			gm = new GM(tamMem, tamFrame);// instancia e inicia o gerenciador de memoria
			// cpu
			cpu = new CPU(m); // cpu acessa memória
		}

	}

	// ------------------- V M - fim
	// ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	public void showConfiguration() {
		System.out.println("\n\n-----------------------------------------------------------");
		System.out.println("*Verificar explicacoes no readme");
		System.out.println("https://github.com/gilbertokoerbes/Trabalho_SiSoP/blob/main/README.md");
		System.out.println("https://github.com/Mateus-P-Oliveira/Trabalho_SiSoP/blob/main/README.md");
		System.out.println("https://github.com/NathanEspindola/Trabalho_SiSoP/blob/main/README.md");

		System.out.println("\nHabilitacao de paginacao dinamica*: " + vm.gm.dynamicOverridePages);
		System.out.println("Habilitacao de frames ocupados para testes*: " + vm.gm.busyFrameTest);
		System.out.println("\n\n---------------------------------------------------------------");
		System.out.print("Memoria total: " + vm.tamMem);
		System.out.print(" | Tamanho dos frames: " + vm.tamFrame);
		System.out.println(" | Total de frames: " + vm.nroFrames);
		System.out.println("--------------------------------------------------------------------\n\n");

	}

	// --------------------H A R D W A R E - fim
	// -------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// ------------------- S O F T W A R E - inicio
	// ----------------------------------------------------------

	// ------------------------------------------- funcoes de um monitor
	public class Monitor {
		GP gp;

		public Monitor() {
			gp = new GP();
		}

		public void dump(Word w) {
			System.out.print("[ ");
			System.out.print(w.opc);
			System.out.print(", ");
			System.out.print(w.r1);
			System.out.print(", ");
			System.out.print(w.r2);
			System.out.print(", ");
			System.out.print(w.p);
			System.out.println("  ] ");
		}

		public void dump(Word[] m, int ini, int fim) {
			for (int i = ini; i < fim; i++) {
				System.out.print(i);
				System.out.print(":  ");
				dump(m[i]);
			}
		}

		/**
		 * Dump frames da memoria
		 */
		public void dumpAllFrames() {

			for (int i = 0; i < vm.nroFrames; i++) {
				System.out.println("----------------------------");
				System.out.println("FRAME " + i + ":");

				vm.gm.dumpFrame(i);

				System.out.println("----------------------------");
			}

		}

		public void dumpId(int id) {
			for (GP.PCB p : monitor.gp.ListProcess) {
				if (p.getId() == id) {
					System.out.println("----------------------------------------");
					System.out.println("PCB Info");
					System.out.println("Process id: " + p.id);
					System.out.println("Atual pc: " + p.pcContext);
					System.out.println("Lista de frames ocupados pelo processo: " + p.tPaginaProcesso.toString());
					System.out.println("Memoria dos frames");
					for (int frame : p.tPaginaProcesso.tabela) {
						vm.gm.dumpFrame(frame);
					}
				}

			}

		}

		public void ps() {
			for (GP.PCB p : monitor.gp.ListProcess) {
				System.out.println("----------------------------------------");
				System.out.print("PCB Info");
				System.out.print("		| Process id: " + p.id);
				System.out.print("		| Atual pc: " + p.pcContext);
				System.out.print("		| Lista de frames ocupados pelo processo: " + p.tPaginaProcesso.toString());
				System.out.println("	| State: " + p.getStateString());

			}

		}

		public void carga(Word[] p, Word[] m, GM.tabelaPaginaProcesso paginasDoProcesso) { // significa ler "p" de
																							// memoria secundaria e
																							// colocar na principal "m"

			for (int i = 0; i < p.length; i++) {
				int t = vm.gm.translate(i, paginasDoProcesso);
				m[t].opc = p[i].opc;
				m[t].r1 = p[i].r1;
				m[t].r2 = p[i].r2;
				m[t].p = p[i].p;
			}
		}

		public void executa(int id) {
			vm.cpu.resetInterrupt(); // zera os interruptores

			GP.PCB CurrentProcess = null;
			for (int i = 0; i < monitor.gp.ListProcess.size(); i++) { // procura o processo pelo id
				if (monitor.gp.ListProcess.get(i).id == id)
					CurrentProcess = monitor.gp.ListProcess.get(i);

			}
			if (CurrentProcess == null) {
				System.out.println("PROGRAMA NAO ENCONTRADO");

			} else {
				gp.CurrentProcessGP = CurrentProcess;
				gp.CurrentProcessGP.setState(STATE.RUNNING);

				vm.cpu.setContext(CurrentProcess.getPc(), CurrentProcess.getTPaginaProcesso(), STATE.RUNNING); // monitor
																												// seta
																												// contexto
				// - pc aponta para
				// inicio do programa
				vm.cpu.run(); // e cpu executa
								// note aqui que o monitor espera que o programa carregado acabe normalmente
								// nao ha protecoes... o que poderia acontecer ?
			}
		}

	}

	// -------------------------------------------
	/**
	 * Gerenciador de processos
	 */
	public class GP {

		/**
		 * ListProcess = lista com todos os processos criados que estão em memoria
		 */
		ArrayList<PCB> ListProcess = new ArrayList<PCB>();
		PCB CurrentProcessGP;

		int uniqueId = 0;

		public int getUniqueId() {
			int idReturn = this.uniqueId;
			this.uniqueId++;
			return idReturn;
		}
		/**
		 * Escalonador implementa FIFS(First-In First-Served)
		 * Nao necessita parametros, pois ira acessar a variavel do processo corrente em execucao
		 */
		public void Escalonador() {
			if(CurrentProcessGP == null)
			CurrentProcessGP.setState(STATE.READY);//(re)coloca o estado atual como pronto

			ArrayList<PCB> ReadyProcess = (ArrayList<Sistema.GP.PCB>) ListProcess.stream()
					.filter(e -> e.getState() == STATE.READY); // Filtra processos em estados pronto

			System.out.println("processos prontos");
			ReadyProcess.stream().forEach(e -> System.out.println(e.getId()));
			
			PCB Escalonado = ReadyProcess.get(0);//obtem o primeiro da fila
			monitor.executa(Escalonado.getId());//executa o primeiro processo filtrado do estado pronto
		}
		private void extracted(Sistema.GP.PCB e) {
			System.out.println(e.getId());
		}

		/**
		 * 
		 * Metodo para criação de processo
		 * 
		 * @param programa
		 * 
		 * @return true: alocação com sucesso
		 * @return false: alocação falhou
		 */
		public int criaProcesso(Word[] programa) {
			int programSize = programa.length;
			if (programSize > vm.tamMem)
				return -1; // verifica o tamanho do programa

			GM.tabelaPaginaProcesso newPages = vm.gm.new tabelaPaginaProcesso();
			boolean sucessAlocation = vm.gm.alocaPaginas(programSize, newPages); // faz alocação dad paginas
			if (sucessAlocation) {
				int id = getUniqueId(); // id igual o ultimo tamanho do array de processos
				monitor.carga(programa, vm.m, newPages);
				PCB P = new PCB(id, 0, newPages);
				ListProcess.add(P);
				return id;
			}

			return -1;

		}

		public void desalocaProcesso(int id) {
			for (int i = 0; i < ListProcess.size(); i++) {
				PCB Process = ListProcess.get(i);
				if (Process.id == id) {
					// remover de todas as listas
					for (Integer framePos : Process.tPaginaProcesso.tabela) { // desaloca os frames ocupados
						vm.gm.frameLivre[framePos] = true;
					}
					ListProcess.remove(Process);

				}
			}
		}

		////////////////////////////////////
		// ---------------PCB----------------/

		public class PCB {
			private int id;
			private int pcContext;
			private STATE state;
			private GM.tabelaPaginaProcesso tPaginaProcesso;

			public PCB(int id, int pc, GM.tabelaPaginaProcesso tPaginaProcesso) {
				this.id = id;
				this.pcContext = pc;
				this.tPaginaProcesso = tPaginaProcesso;
				this.state = STATE.READY;
			}

			public int getId() {
				return this.id;
			}

			public int getPc() {
				return this.pcContext;
			}

			public void setPc(int newPc) {
				this.pcContext = newPc;
			}

			public void setState(STATE state) {
				this.state = state;
			}

			public STATE getState() {
				return this.state;
			}

			public String getStateString() {
				switch (this.state) {
					case READY:
						return "Ready";
					case RUNNING:
						return "Running";
					default:
						return "STATE FAILED!";

				}
			}

			public GM.tabelaPaginaProcesso getTPaginaProcesso() {
				return this.tPaginaProcesso;

			}

		}
	}

	public class GM {
		int tamMem;
		int tamFrame;
		boolean frameLivre[];

		/**
		 * habilita a alocação de paginas durante execução PODE GERAR SOBRESCRITA! \n
		 * Ex: caso o programa tente acessar uma posição de memória ou pagina que não é
		 * sua, é verificado se está disponivel.
		 * Se disponivel, ira alocar uma nova pagina para o processo poder prosseguir
		 * 
		 * @return "Um aviso de nova alocação é informado"
		 */
		public boolean dynamicOverridePages = true;

		/**
		 * Variavel para simular sem programas a ocupacao de frames de forma intercala.
		 * {@literal} Objetivo: mostrar e comprovar o comportamento do sistema.
		 * 
		 */
		public boolean busyFrameTest = true;

		public GM(int tamMem, int tamFrame) {
			this.tamFrame = tamFrame;
			this.tamMem = tamMem;

			int nroFrames = tamMem / tamFrame;
			frameLivre = new boolean[nroFrames]; // seta o tamanho do array de frame

			if (busyFrameTest) {
				System.out.println("*BUSY FRAME TEST ACTIVE!"); // avisa sobre o teste ativo
				System.out.println("Frames alocados como ocupados: ");
			}
			for (int i = 0; i < nroFrames; i++) {// inicia todos os frames em true
				frameLivre[i] = true;

				if (i % 2 == 0 && busyFrameTest) {
					System.out.print(" | " + i);
					frameLivre[i] = false; // test case
				}
			}

		}

		public boolean alocaPaginas(int nroPalavras, tabelaPaginaProcesso paginas) {
			int nroframeslivre = 0; // verifica o total de frames livres
			int paginasRequeridas = nroPalavras / this.tamFrame;

			int offset = nroPalavras % this.tamFrame; // caso haja divisao quebrada, necessita mais uma pagina
			if (offset != 0)
				paginasRequeridas++;

			for (boolean b : frameLivre) { // calculando frames livres
				if (b == true)
					nroframeslivre++;
			}

			if (paginasRequeridas <= nroframeslivre) { // verifica se há todos os frames necessários, se houve, ira
														// carrega-los

				while (paginasRequeridas >= 0) { // aloca frames enquanto necessario

					for (int i = 0; i < frameLivre.length; i++) {// iterar sobre o array de frames
						if (frameLivre[i] == true) {
							frameLivre[i] = false;
							paginas.tabela.add(i); // adiciona o frame 'i' no vetor das paginas do processo
							paginasRequeridas--;
							break;
						}
					}
				}
				return true;
			}

			return false;
		}

		public void desalocaPaginas(tabelaPaginaProcesso paginas) {
			for (int p : paginas.tabela) {
				frameLivre[p] = true;
			}

		}

		public void dumpFrame(int frame) {
			System.out.println("FrameLivre: " + vm.gm.frameLivre[frame]);

			int pInicio = frame * vm.tamFrame;
			int pFim = pInicio + tamFrame;
			monitor.dump(vm.m, pInicio, pFim);
		}

		/**
		 * Tradutor do endereco lógico
		 * input: object tabela / int endereco logico
		 * * @return int posicao real na memoria
		 */
		public int translate(int posicaoSolicitada, tabelaPaginaProcesso t) {
			int totalFrames = t.tabela.size();
			int p = posicaoSolicitada / tamFrame; // p = contagem de posicao no array da tabela
			int offset = posicaoSolicitada % tamFrame; // offset desclocamente dentro do frame

			if (p >= totalFrames && this.dynamicOverridePages) { // verifica se durante a exexcução foi requisitado
																	// algum endereco fora do escopo de paginas
				boolean sucessNewAllocaded = alocaPaginas(1, t); // aloca nova pagina para posição
				if (sucessNewAllocaded) {
					System.out.println("warning: new page is allocated");
				} else {
					vm.cpu.interrupcaoAtiva = interrupt.InvalidAdrress; // se nao conseguiu alocar, retorna problema de
																		// acesso a memoria
				}
			}

			int frameInMemory = t.tabela.get(p); // obtem no indice de paginas o frame real da memoria
			int positionInMemory = tamFrame * frameInMemory + offset;

			return positionInMemory;
		}

		/**
		 * Tabela das paginas do processos
		 */
		public class tabelaPaginaProcesso { // classe para modulalizar como objeto as tabelas. Cada processo possui sua
			// tabela
			ArrayList<Integer> tabela;

			public tabelaPaginaProcesso() {
				tabela = new ArrayList<>();
			}

			@Override
			public String toString() {
				String output = "";
				for (Integer i : tabela) {
					output += " | " + i + " | ";
				}

				return output;
			}

		}

	}

	/**
	 * States do processo, utilizado por GP
	 * 
	 * @see Sistema.GP
	 */
	public enum STATE {
		RUNNING,
		READY,
		BLOCKED;
	}

	// -------------------------------------------------------------------------------------------------------
	// ------------------- S I S T E M A
	// --------------------------------------------------------------------

	public VM vm;
	public Monitor monitor;
	public static Programas progs;

	public Sistema() { // a VM com tratamento de interrupções
		vm = new VM();
		monitor = new Monitor();
		progs = new Programas();

	}

	public class Terminal extends Thread {
		Sistema s;

		public Terminal(Sistema s) {
			this.s = s;
		}

		public void run() {
			boolean SystemRun = true;
			Scanner scanner = new Scanner(System.in);
			while (SystemRun) {

				System.out.print("~$terminal: ");
				String inputConsole = scanner.nextLine();
				String inputParams[] = inputConsole.split(" ");
				int id = -1; // id de processo quando solicitado
				try {
					switch (inputParams[0]) {

						case "cria":
							System.out.println("Processo solicitado = " + inputParams[1]);
							int idNewProcess = -1;
							switch (inputParams[1]) {

								case "PA":
									idNewProcess = s.monitor.gp.criaProcesso(progs.PA);
									System.out.println("id = " + idNewProcess);
									break;
								case "PB":
									idNewProcess = s.monitor.gp.criaProcesso(progs.PB);
									System.out.println(idNewProcess);
									break;

								case "PC":
									idNewProcess = s.monitor.gp.criaProcesso(progs.PC);
									System.out.println(idNewProcess);
									break;
								case "testIN":
									idNewProcess = s.monitor.gp.criaProcesso(progs.testIN);
									System.out.println(idNewProcess);
									break;
								case "testOUT":
									idNewProcess = s.monitor.gp.criaProcesso(progs.testOUT);
									System.out.println(idNewProcess);
									break;
								case "testInvalidAdrress":
									idNewProcess = s.monitor.gp.criaProcesso(progs.testInvalidAdrress);
									System.out.println(idNewProcess);
									break;
								case "testOverFlow":
									idNewProcess = s.monitor.gp.criaProcesso(progs.testOverFlow);
									System.out.println(idNewProcess);
									break;

								default:
									System.out.println("Programa invalido ou inexistente.");
									break;
							}
							break; // break criaProcesso

						case "executa":
							//id = Integer.parseInt(inputParams[1]);
							//s.monitor.executa(id);
							System.out.println("A EXECUTAR");
							monitor.gp.Escalonador();
							break;

						case "dump":
							id = Integer.parseInt(inputParams[1]);
							s.monitor.dumpId(id);
							break;
						case "dumpM":
							int ini = Integer.parseInt(inputParams[1]);
							int fim = Integer.parseInt(inputParams[2]) + 1; // correcao para comtemplar a ultima posicao
																			// solicitada
							s.monitor.dump(vm.m, ini, fim);
							break;

						case "desaloca":
							id = Integer.parseInt(inputParams[1]);
							s.monitor.gp.desalocaProcesso(id);
							;
							break;
						case "dumpAllFrames":
							s.monitor.dumpAllFrames();// exibit todos os frames
							break;
						case "dumpFrame":
							int frame = Integer.parseInt(inputParams[1]);
							vm.gm.dumpFrame(frame);
							break;
						case "ps":
							s.monitor.ps();
							break;
						case "exit":
							System.out.println("Bye!");
							SystemRun = false;
							break;
						case "":
							break;
						default:
							System.out.println("Parametro invalido. Verifique em READM");
							break;

					}
				} catch (Exception e) {// excecoes de entrada. Forma interativa

					System.out.println(inputParams[0]);

					String tab = ""; // deslocar as escritas de argumentos
					while (tab.length() <= inputParams[0].length())
						tab += " ";

					System.out.println(tab + "^^^");
					System.out.println(tab + "argumentos invalidos para solicitacao. Verifique em README");
				}

			}

		}

	}

	/*
	 * /**
	 * 
	 * @deprecated
	 * 
	 * @param id
	 * public void roda(int id) { // metodo roda modificado
	 * // Instanciar um objeto para conter a tabela de paginas desse processo. Essa
	 * // tabela irá ser manipulada pelo GM
	 * 
	 * monitor.dump(vm.m, 0, 90); // Muda o total do Dump
	 * monitor.executa(id);
	 * System.out.println("---------------------------------- após execucao ");
	 * monitor.dump(vm.m, 0, 90); // Muda o total do Dump
	 * }
	 */

	public void trataTnterrupcoes(interrupt i) {
		System.out.print("I-N-T-E-R-R-U-P-T-I-O-N -> ");
		if (i == interrupt.InvalidAdrress)
			System.out.println("Acesso invalido a memoria");
		if (i == interrupt.InvalidOpcode)
			System.out.println("Opcode invalido");
		if (i == interrupt.Overflow)
			System.out.println("OverFlow");

		if (i == interrupt.Stop) {
			monitor.gp.desalocaProcesso(monitor.gp.CurrentProcessGP.getId());
			System.out.println("Fim da execucao do programa");
		}

		if (i == interrupt.Timer) {
			System.out.println("Escalonamento");
			monitor.gp.Escalonador(); // chama o escalonador

		}
	}

	// ------------------- S I S T E M A - fim
	// --------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// ------------------- instancia e testa sistema
	public static void main(String args[]) {
		Sistema s = new Sistema();
		s.showConfiguration();
		Terminal terminal = s.new Terminal(s);
		terminal.start();
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------- TUDO ABAIXO DE MAIN É AUXILIAR PARA FUNCIONAMENTO DO SISTEMA
	// - nao faz parte

	// -------------------------------------------- programas aa disposicao para
	// copiar na memoria (vide carga)
	public class Programas {
		/*
		 * public Word[] progMinimo = new Word[] {
		 * // OPCODE R1 R2 P :: VEJA AS COLUNAS VERMELHAS DA TABELA DE DEFINICAO DE
		 * OPERACOES
		 * // :: -1 SIGNIFICA QUE O PARAMETRO NAO EXISTE PARA A OPERACAO DEFINIDA
		 * new Word(Opcode.LDI, 0, -1, 999),
		 * new Word(Opcode.STD, 0, -1, 10),
		 * new Word(Opcode.STD, 0, -1, 11),
		 * new Word(Opcode.STD, 0, -1, 12),
		 * new Word(Opcode.STD, 0, -1, 13),
		 * new Word(Opcode.STD, 0, -1, 14),
		 * new Word(Opcode.STOP, -1, -1, -1) };
		 * 
		 * public Word[] fibonacci10 = new Word[] { // mesmo que prog exemplo, so que
		 * usa r0 no lugar de r8
		 * new Word(Opcode.LDI, 1, -1, 0),
		 * new Word(Opcode.STD, 1, -1, 20), // 20 posicao de memoria onde inicia a serie
		 * de fibonacci gerada
		 * new Word(Opcode.LDI, 2, -1, 1),
		 * new Word(Opcode.STD, 2, -1, 21),
		 * new Word(Opcode.LDI, 0, -1, 22),
		 * new Word(Opcode.LDI, 6, -1, 6),
		 * new Word(Opcode.LDI, 7, -1, 30),
		 * new Word(Opcode.LDI, 3, -1, 0),
		 * new Word(Opcode.ADD, 3, 1, -1),
		 * new Word(Opcode.LDI, 1, -1, 0),
		 * new Word(Opcode.ADD, 1, 2, -1),
		 * new Word(Opcode.ADD, 2, 3, -1),
		 * new Word(Opcode.STX, 0, 2, -1),
		 * new Word(Opcode.ADDI, 0, -1, 1),
		 * new Word(Opcode.SUB, 7, 0, -1),
		 * new Word(Opcode.JMPIG, 6, 7, -1),
		 * new Word(Opcode.STOP, -1, -1, -1), // POS 16
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1), // POS 20
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1),
		 * new Word(Opcode.DATA, -1, -1, -1) // ate aqui - serie de fibonacci ficara
		 * armazenada
		 * };
		 * 
		 * public Word[] fatorial = new Word[] { // este fatorial so aceita valores
		 * positivos. nao pode ser zero
		 * // linha coment
		 * new Word(Opcode.LDI, 0, -1, 6), // 0 r0 é valor a calcular fatorial
		 * new Word(Opcode.LDI, 1, -1, 1), // 1 r1 é 1 para multiplicar (por r0)
		 * new Word(Opcode.LDI, 6, -1, 1), // 2 r6 é 1 para ser o decremento
		 * new Word(Opcode.LDI, 7, -1, 8), // 3 r7 tem posicao de stop do programa = 8
		 * new Word(Opcode.JMPIE, 7, 0, 0), // 4 se r0=0 pula para r7(=8)
		 * new Word(Opcode.MULT, 1, 0, -1), // 5 r1 = r1 * r0
		 * new Word(Opcode.SUB, 0, 6, -1), // 6 decrementa r0 1
		 * new Word(Opcode.JMP, -1, -1, 4), // 7 vai p posicao 4
		 * new Word(Opcode.STD, 1, -1, 10), // 8 coloca valor de r1 na posição 10
		 * new Word(Opcode.STOP, -1, -1, -1), // 9 stop
		 * 
		 * new Word(Opcode.DATA, -1, -1, -1) }; // 10 ao final o valor do fatorial
		 * estará na posição 10 da memória
		 */

		public Word[] PB = new Word[] {

				new Word(Opcode.LDI, 0, -1, 4), // 0 Valor armazenado na memoria
				new Word(Opcode.LDI, 1, -1, 13), // 1 Linha do salto do jump de 0
				new Word(Opcode.LDI, 2, -1, 7), // 2 Linha do salto do jump do loop
				new Word(Opcode.LDI, 3, -1, 1), // 3 Valor do inicio do Fatoral
				new Word(Opcode.LDI, 4, -1, -1), // 4 Valor caso seja negativo
				new Word(Opcode.LDI, 5, -1, 11), // 5
				// Teste do Zero
				new Word(Opcode.JMPIL, 1, 0, -1), // 6 Se R0 for menor que 0 salta pro fim
				// LOOP do Fatorial
				new Word(Opcode.JMPIE, 5, 0, -1), // 7
				new Word(Opcode.MULT, 3, 0, -1), // 8 Multiplico o valor dele por ele mesmo
				new Word(Opcode.SUBI, 0, -1, 1), // 9 Diminuo o valor do numero fatoral
				new Word(Opcode.JMPGE, 2, 0, -1), // 10 coloca valor de r1 na posição 10
				// FIM do progama para o Fatorial
				new Word(Opcode.STD, 3, -1, 20), // 11 Armazeno na memoria o resultado do faotrial
				new Word(Opcode.STOP, -1, -1, -1), // 12 Termina o progama
				// FIM do programa para o Negativo
				new Word(Opcode.STD, 4, -1, 20), // 13 Salvo -1 no inicio da memoria
				new Word(Opcode.STOP, -1, -1, -1) }; // 14 Termina o progama

		public Word[] testOverFlow = new Word[] {
				new Word(Opcode.LDI, 0, -1, 2147483647), // 0 Valor armazenado na memoria
				new Word(Opcode.LDI, 1, -1, 2147483647), // 1 Valor armazenado na memoria
				new Word(Opcode.MULT, 0, 1, -1) // 2 aplica multiplicacao (forcando overflow)
		};

		public Word[] testInvalidOpcode = new Word[] {
				new Word(Opcode.DATA, -1, -1, -1), // 0 DATA nao e opcode de CPU
		};

		public Word[] testInvalidAdrress = new Word[] {
				new Word(Opcode.STD, 0, -1, 2048), // 0 2048 Endereco invalido (Ex de max 1024)
		};

		public Word[] testIN = new Word[] {
				new Word(Opcode.LDI, 8, -1, 1),
				new Word(Opcode.LDI, 9, -1, 4),
				new Word(Opcode.TRAP, -1, -1, -1),
				new Word(Opcode.STOP, -1, -1, -1),
		};
		public Word[] testOUT = new Word[] {
				new Word(Opcode.LDI, 0, -1, 12345),
				new Word(Opcode.STD, 0, -1, 10),
				new Word(Opcode.LDI, 8, -1, 2),
				new Word(Opcode.LDI, 9, -1, 10),
				new Word(Opcode.TRAP, -1, -1, -1),
				new Word(Opcode.STOP, -1, -1, -1),
		};

		public Word[] PA = new Word[] {
				new Word(Opcode.LDI, 0, -1, 4), // Input da repeticao
				new Word(Opcode.LDI, 1, -1, 28),
				new Word(Opcode.LDI, 2, -1, 0),
				new Word(Opcode.LDI, 3, -1, 27),
				new Word(Opcode.LDI, 4, -1, 1),
				new Word(Opcode.LDI, 5, -1, 32),
				// JMP Teste do zero
				new Word(Opcode.JMPIL, 1, 0, -1),
				new Word(Opcode.LDI, 6, -1, 0),
				new Word(Opcode.STX, 5, 6, -1),
				new Word(Opcode.ADDI, 5, -1, 1),
				new Word(Opcode.SUBI, 0, -1, 1),
				new Word(Opcode.JMPIE, 3, 0, -1),
				new Word(Opcode.STX, 5, 4, -1),
				new Word(Opcode.ADDI, 5, -1, 1),
				new Word(Opcode.SUBI, 0, -1, 1),
				// LOOP Fibonacci
				new Word(Opcode.JMPIE, 3, 0, -1),
				new Word(Opcode.SUBI, 0, -1, 1),
				new Word(Opcode.ADD, 6, 4, -1),
				new Word(Opcode.STX, 5, 6, -1),
				new Word(Opcode.ADDI, 5, -1, 1),
				new Word(Opcode.LDI, 2, -1, 0),
				new Word(Opcode.ADD, 2, 6, -1),
				new Word(Opcode.LDI, 6, -1, 0),
				new Word(Opcode.ADD, 6, 4, -1),
				new Word(Opcode.LDI, 4, -1, 0),
				new Word(Opcode.ADD, 4, 2, -1),
				new Word(Opcode.JMP, -1, -1, 15),
				// Fim do programa
				new Word(Opcode.STOP, -1, -1, -1),
				// Fim do programa se for um zero
				new Word(Opcode.LDI, 2, -1, -1),
				new Word(Opcode.STD, 2, -1, 32),
				new Word(Opcode.STOP, -1, -1, -1)
		};

		public Word[] PC = new Word[] {
				// coloca elementos no vetor
				new Word(Opcode.LDI, 0, -1, 8),
				new Word(Opcode.STD, 0, -1, 60),
				new Word(Opcode.LDI, 0, -1, 7),
				new Word(Opcode.STD, 0, -1, 61),
				new Word(Opcode.LDI, 0, -1, 6),
				new Word(Opcode.STD, 0, -1, 62),
				new Word(Opcode.LDI, 0, -1, 5),
				new Word(Opcode.STD, 0, -1, 63),
				new Word(Opcode.LDI, 0, -1, 4),
				new Word(Opcode.STD, 0, -1, 64),
				new Word(Opcode.LDI, 0, -1, 3),
				new Word(Opcode.STD, 0, -1, 65),
				new Word(Opcode.LDI, 0, -1, 2),
				new Word(Opcode.STD, 0, -1, 66),
				new Word(Opcode.LDI, 0, -1, 1),
				new Word(Opcode.STD, 0, -1, 67),
				// Pega o tamanho do vetor
				new Word(Opcode.LDI, 8, -1, 8), // Tamanho do vetor
				new Word(Opcode.LDI, 9, -1, 2),
				new Word(Opcode.MULT, 8, 9, -1),
				new Word(Opcode.ADDI, 8, -1, 1), // Repeticoes
				new Word(Opcode.LDI, 9, -1, 52),
				// Loop Externo
				new Word(Opcode.SUBI, 8, -1, 1),
				new Word(Opcode.JMPIE, 9, 8, -1),
				// Associa os Reg
				new Word(Opcode.LDI, 0, -1, 60),
				new Word(Opcode.LDI, 7, -1, 61),
				new Word(Opcode.LDI, 1, -1, 51),
				new Word(Opcode.LDX, 2, 0, -1),
				new Word(Opcode.LDX, 3, 7, -1),
				new Word(Opcode.LDI, 4, -1, 0),
				new Word(Opcode.LDI, 5, -1, 46),
				new Word(Opcode.LDI, 6, -1, 8), // Tamanho do vetor
				// Aumento o vetor
				new Word(Opcode.ADDI, 6, -1, 0),
				new Word(Opcode.JMP, -1, -1, 35),
				// Loop principal
				new Word(Opcode.ADDI, 0, -1, 1),
				new Word(Opcode.ADDI, 7, -1, 1),
				new Word(Opcode.LDX, 2, 0, -1),
				new Word(Opcode.LDX, 3, 7, -1),
				new Word(Opcode.SUBI, 6, -1, 1),
				new Word(Opcode.LDI, 4, -1, 0),
				new Word(Opcode.ADD, 4, 2, -1),
				new Word(Opcode.JMPIE, 1, 6, -1),
				new Word(Opcode.SUB, 2, 3, -1),
				new Word(Opcode.JMPIG, 5, 2, -1),
				new Word(Opcode.LDI, 2, -1, 0),
				new Word(Opcode.ADD, 2, 4, -1),
				new Word(Opcode.JMP, -1, -1, 33),
				// Bubble Sort
				new Word(Opcode.STX, 0, 3, -1),
				new Word(Opcode.STX, 7, 4, -1),
				new Word(Opcode.LDI, 2, -1, 0),
				// new Word(Opcode.LDI, 4, -1, 0),
				new Word(Opcode.ADD, 2, 4, -1),
				new Word(Opcode.JMP, -1, -1, 33),
				// JMP para o loop externo
				new Word(Opcode.JMP, -1, -1, 21),
				// FIM DO PROGRAMA
				new Word(Opcode.STOP, -1, -1, -1)
		};

	}
}
