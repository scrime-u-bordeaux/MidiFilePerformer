package fr.inria.midifileperformer.impl;

import java.io.File;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Pair;
import fr.inria.bps.base.Vecteur;
import fr.inria.fun.Proc1;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.Lib;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.EndOfStream;
import fr.inria.midifileperformer.core.H;
import fr.inria.midifileperformer.core.Peek;
import fr.inria.midifileperformer.core.Record;
import fr.inria.midifileperformer.core.Rendering;
import fr.inria.midifileperformer.core.S;

public class MidiRendering {
	public static String demoSong = "Internal demo song";
	public static NearFilter forPierre = new NearFilter();
	
	/*
	 * Main function
	 */
	public static void launch(Config config, Proc1<Long> showTime) {
		//System.out.println("launch");
		C<MidiMsg> in0 = readAndFilter(config, config.filename.toString(), showTime);
		//C<Vector<Event<MidiMsg>>> acin = old_analysis(config, in0);
		C<Vector<Event<MidiMsg>>> acin = doAnalysis(config, in0);
		C<Vector<Event<MidiMsg>>> partition = traceOn(acin, t -> showTime.operation(t));
		C<Vector<Event<MidiMsg>>> cin;
		if(config.loop) {
			cin = C.loop(() -> partition);
		} else {
			cin = partition ;
		}
		
		C<MidiMsg> control1 = collector(config.inputs);
		C<MidiMsg> control;
		if(config.adjacentKeyFilter) {
			control = control1.filter(x -> forPierre.filter(x.value));
		} else {
			control = control1;
		}
		C<Vector<Event<MidiMsg>>> renderingResult = Rendering.mergeBeginEnd(cin, control, 
				Lib.toVector(Lib.mergeEvent(MidiMsg.merge)));
		Record<MidiMsg> record = Record.make();
		config.record = record;
		config.byPass = new LinkedBlockingQueue<MidiMessage>();
		config.player = MidiRendering.launchPlayer(config, renderingResult);
	}

	public static C<Vector<Event<MidiMsg>>> doAnalysis(Config config, C<MidiMsg> in) {
		Analysis r = analysis(config, in.toH());
		return(C.make(r.offOn.values).map(e->e.convert(h -> h.values)));
	}
	
	static Analysis analysis(Config config, H<MidiMsg> in) {
		//System.out.println("analysis... "+config.analysis_channel);
		if(config.analysis_channel) {
			Pair<H<MidiMsg>,H<MidiMsg>> p = Analysis.splitChannel(in, config.selected_channel);
			return(inAnalysis(config, p.car).unsplit(p.cdr));
		} else {
			return(inAnalysis(config, in));
		}
	}
	
	static Analysis inAnalysis(Config config, H<MidiMsg> in) {
		if(config.analysis_count_on) {
			System.out.println("cont_on "+config.analysis_count_on);
			return(Analysis.countOn(in, config.nb_on).addOff().addFirst());
		}
		if(config.analysis_slice) {
			int n = config.slice_size;
			Analysis r;
			if(config.slice_strict) {
				System.out.println("strict slice of "+n);
				r = Analysis.strictSlice(in, n);
			} else {
				System.out.println("slice of "+n);
				r = Analysis.slice(in, n);
			}
			return(r.addOff().addFirst());
		}
		Analysis r = Analysis.sing(in);
		if(config.doSync) {
			//System.out.println("Sync");
			r = r.sync();
		}
		//System.out.println("On separation");
		r = r.OnSeparation();
		if(config.max_delay > 0) {
			System.out.println("CompressDelay "+config.max_delay);
			r = r.compressDelay(config.max_delay);
		}
		if(config.unmeet) {
			//System.out.println("unmeet "+config.unmeet);
			r = r.unmeet();
		}
		if(config.analysis_sync_off) {
			System.out.println("sync off "+config.analysis_sync_off);
			r = r.syncOff();
		}
		return(r);
	}
	
	
	/*
	 * Old version : C<H<MidiMsg>>
	 */

	static C<Vector<Event<MidiMsg>>> old_analysis(Config config, C<MidiMsg> in) {
		if(config.analysis_sync_off) return(Lib.stdAnalysis(in, config.unmeet));
		//if(config.analysis_unsync_off) return(Lib.unstdAnalysis(in, config.unmeet));
		if(config.analysis_count_on) return(Lib.countOnAnalysis(in, config.nb_on));
		if(config.analysis_slice) return(Lib.beatAnalysis(in, config.slice_size));
		if(config.analysis_channel) return(Lib.chanAnalysis(in, config.selected_channel));
		System.out.println("no analysis");
		return(null);
	}
	
	C<Vector<MidiMsg>> doSync(Config config, C<MidiMsg> in) {
		if(config.separateChord) {
			return(in.map(e -> Event.make(e.time, Vecteur.sing(e.value))));
		} else {
			return(in.sync());
		}
	}
	

	public static C<MidiMsg> collector(Vector<InputDevice> inputs) {
		return(S.collect(InputDevice.hub).live());
	}

	public static C<MidiMsg> readAndFilter(Config config, String filename, Proc1<Long> showTime) {
		C<MidiMsg> r1 = readfile(filename, showTime).filter(e -> filter(config, e)); 
		return(C.make(MidiMsg.allNotesOff, 0).seq(r1));
	}

	public static C<Vector<MidiMsg>> _traceOn(C<Vector<MidiMsg>> in, Proc1<Long> showTime) {
		return(new C<Vector<MidiMsg>>() {

			public Event<Vector<MidiMsg>> get() throws EndOfStream {
				Event<Vector<MidiMsg>> event = in.get();
				if(Vecteur.any(event.value, x-> x.isBegin()))
					showTime.operation(event.time);
				return(event);
			}
		});
	}

	public static C<Vector<Event<MidiMsg>>> traceOn(C<Vector<Event<MidiMsg>>> in, Proc1<Long> showTime) {
		return(new C<Vector<Event<MidiMsg>>>() {

			public Event<Vector<Event<MidiMsg>>> get() throws EndOfStream {
				Event<Vector<Event<MidiMsg>>> event = in.get();
				if(Vecteur.any(event.value, x-> x.value.isBegin()))
					showTime.operation(event.time);
				return(event);
			}
		});
	}
	
	//Vector<MidiMsg> v = MidiMsg.allNotesOff;
	//for(int i=0; i<v.size(); i++) r.add(Event.make(0, v.get(i)));

	//	public static C<MidiMsg> readfile(String filename) {
	//	return(C.make(readv(filename)));
	//}
	public static C<MidiMsg> readfile(String filename, Proc1<Long> showTime) {
		return(makeAndGetNext(readv(filename), showTime));
	}

	public static <T> C<MidiMsg> makeAndGetNext(Vector<Event<MidiMsg>> events, Proc1<Long> showTime) {
		return(new C<MidiMsg>() {
			int i=0;
			public Event<MidiMsg> get() throws EndOfStream {
				if(i >= events.size()) throw(new EndOfStream("End of data"));
				//showTime.operation(MidiMsg.nextOn(events, i));
				Event<MidiMsg> r = events.get(i++);
				//if(r.value.isBegin()) showTime.operation(r.time);
				return(r);
			}
		});
	}

	public static Vector<Event<MidiMsg>> readv(String filename) {
		if(filename.compareTo(demoSong) == 0) return(SimpleMidiFile.demov());
		try {
			return(Midi.getEvents(new File(filename)));
		} catch(Exception e) {
			try {
				return(SimpleMidiFile.parse(filename));
			} catch(Exception ee) {
				System.out.println("connot read file - use a demo instead");
				return(SimpleMidiFile.demov());
			}
		}
	}

	public static boolean filter(Config config, Event<MidiMsg> e) {
		if(e.time < config.start) return(false);
		if(config.stateChannels[MidiLib.getChannel(e.value.msg)] < 0) return(false);
		if(MidiLib.isPedal(e.value.msg) && !config.keepPedal) return(false);
		if(MidiLib.changeTempo(e.value.msg) != -1 && !config.keepTempo) return(false);
		if(config.stop < 0) return(true);
		return(e.time < config.stop);
	}

	public static void urgent(Config config, MidiMessage msg) {
		config.byPass.offer(msg);
	}

	//public static double tempo = 1.0;
	public static Thread launchPlayer(Config config, C<Vector<Event<MidiMsg>>> toPlay) {
		Vector<OutputDevice> outputs = config.outputs;
		//C<MidiMsg> toPlay = config.record;
		LinkedBlockingQueue<MidiMessage> byPass = config.byPass;
		Thread player = new Thread() {
			boolean[][] pressed = new boolean[16][128];
			int[][] released = new int[16][128];
			public void run() {
				for(int i=0; i<16; i++) {
					boolean[] pc =new boolean[128];
					for(int j=0; j<128; j++) pc[j] = false;
					pressed[i] = pc;
				}
				for(int i=0; i<16; i++) {
					int[] pr =new int[128];
					for(int j=0; j<128; j++) pr[j] = 0;
					released[i] = pr;
				}
				try {
					//System.out.println("player launch");
					//for(OutputDevice dev : outputs) dev.open();
					//toPlay.distribute(outputs);
					distribute(toPlay, outputs);
				} catch (Exception e) {
				}
			}

			public void distribute(C<Vector<Event<MidiMsg>>> toPlay, Vector<OutputDevice> outputs) {
				try {
					while(true) {
						if(!byPass.isEmpty()) {
							MidiMessage msg = byPass.take();
							for(OutputDevice cons : outputs) cons.accept(Event.make(-1, new MidiMsg(msg)));
						} else {
							//System.out.println("to "+toPlay);
							Event<Vector<Event<MidiMsg>>> event = toPlay.get();
							//System.out.println("ev "+event);
							if(config.detachPart) { 
								new Thread() {
									public void run() {
										distributePartition(event);
									}
								}.start();
							} else {
								distributePartition(event);
							}
						}
					}
				} catch (EndOfStream e) {
				} catch (Exception e) {
					System.out.println("The player is dead "+e);
				}
			}
			public void distributePartition(Event<Vector<Event<MidiMsg>>> event) {
				Vector<Event<MidiMsg>> v = event.value;
				//System.out.println("Sub partition "+v.size());
				long time = event.time;
				for( Event<MidiMsg> e : v ) {
					long etime = e.time;
					long dtime = etime-time;
					if(dtime > 0) {
						if(config.setTempoByOn) config.resetTempo(Rendering.tempo);
						long ndtime = (long) (dtime*config.tempo);
						//System.out.println("Sleep "+Rendering.tempo+" "+dtime+" "+ndtime);
						Lib.sleep(ndtime);
					}
					time = etime;
					config.record.addNow(e.value);
					distributeEvent(e);
				}
			}
			public void distributeEvent(Event<MidiMsg> event) {
				MidiMessage msg = event.value.msg;
				int channel = (msg.getStatus() & 0xF);
				boolean send = true;
				if(MidiLib.isBegin(msg)) {
					ShortMessage m = (ShortMessage) msg;
					int k = m.getData1();
					if(pressed[channel][k]) {
						MidiMsg off = MidiMsg.NoteOff(channel, k, 0);
						Event<MidiMsg> oevent = Event.make(event.time, off);
						//System.out.println("force off "+oevent);
						for(OutputDevice cons : outputs) cons.accept(oevent);
						released[channel][k]++;
					}
					pressed[channel][k] = true;
				} else if(MidiLib.isEnd(msg)) {
					ShortMessage m = (ShortMessage) msg;
					int k = m.getData1();
					if(released[channel][k] > 0) {
						released[channel][k]--;
						send = false;
					}
					if(released[channel][k] == 0) pressed[channel][k] = false;
				}
				if(send) for(OutputDevice cons : outputs) cons.accept(event);
			}
		};
		player.start();
		return(player);
	}

	public static Thread _launchPlayer(Config config) {
		Vector<OutputDevice> outputs = config.outputs;
		//C<MidiMsg> toPlay = config.record;
		C<MidiMsg> toPlay = null;
		LinkedBlockingQueue<MidiMessage> byPass = config.byPass;
		Thread player = new Thread() {
			boolean[][] pressed = new boolean[16][128];
			int[][] released = new int[16][128];
			public void run() {
				for(int i=0; i<16; i++) {
					boolean[] pc =new boolean[128];
					for(int j=0; j<128; j++) pc[j] = false;
					pressed[i] = pc;
				}
				for(int i=0; i<16; i++) {
					int[] pr =new int[128];
					for(int j=0; j<128; j++) pr[j] = 0;
					released[i] = pr;
				}
				try {
					//System.out.println("player launch");
					//for(OutputDevice dev : outputs) dev.open();
					//toPlay.distribute(outputs);
					distribute(toPlay, outputs);
				} catch (Exception e) {
				}
			}

			public void distribute(C<MidiMsg> toPlay, Vector<OutputDevice> outputs) {
				try {
					System.out.println("to "+toPlay);
					while(true) {
						if(!byPass.isEmpty()) {
							MidiMessage msg = byPass.take();
							for(OutputDevice cons : outputs) cons.accept(Event.make(-1, new MidiMsg(msg)));
						} else {
							Event<MidiMsg> event = toPlay.get();
							MidiMessage msg = event.value.msg;
							int channel = (msg.getStatus() & 0xF);
							boolean send = true;
							if(MidiLib.isBegin(msg)) {
								ShortMessage m = (ShortMessage) msg;
								int k = m.getData1();
								if(pressed[channel][k]) {
									MidiMsg off = MidiMsg.NoteOff(channel, k, 0);
									Event<MidiMsg> oevent = Event.make(event.time, off);
									//System.out.println("force off "+oevent);
									for(OutputDevice cons : outputs) cons.accept(oevent);
									released[channel][k]++;
								}
								pressed[channel][k] = true;
							} else if(MidiLib.isEnd(msg)) {
								ShortMessage m = (ShortMessage) msg;
								int k = m.getData1();
								if(released[channel][k] > 0) {
									released[channel][k]--;
									send = false;
								}
								if(released[channel][k] == 0) pressed[channel][k] = false;
							}
							if(send) for(OutputDevice cons : outputs) cons.accept(event);
						}
					}
				} catch (EndOfStream e) {
				} catch (Exception e) {
					System.out.println("The player is dead "+e);
				}
			}
		};
		player.start();
		return(player);
	}


	/*
	 * Work on vectors
	 */
	public static void hlaunch(Config config, Proc1<Long> showTime) {
		//H<H<MidiMsg>> p = vanalysis(config, vreadAndFilter(config));
		//C<MidiMsg> ctrl = vctrl(config);
	}
	static H<MidiMsg> vreadAndFilter(Config config) {
		String filename = config.filename.toString();
		return(new H<MidiMsg>(Vecteur.filter(readv(filename), e -> filter(config, e))));
	}
	
	static H<H<MidiMsg>> vanalysis(Config config, H<MidiMsg> v) {
		//if(config.analysis_sync_off) return(Lib.vstdAnalysis(config, v));
		//if(config.analysis_unsync_off) return(Lib.unstdAnalysis(in, config.unmeet));
		if(config.analysis_count_on) return(countOnAnalysis(config, v, 8));
		//if(config.analysis_beat) return(Lib.cbeatAnalysis(in, 1617));
		//if(config.analysis_channel)
		return(null);
	}
	
	static H<H<MidiMsg>> countOnAnalysis(Config config, H<MidiMsg> v, int count) {
		H<H<MidiMsg>> r = new H<H<MidiMsg>>();
		int i = 0;
		int n = v.size();
		while(i<n) {
			int seen = 0;
			H<MidiMsg> sr = new H<MidiMsg>(2*count);
			while(seen < count && i < n) {
				Event<MidiMsg> e = v.get(i++);
				sr.add(e);
				if(e.value.isBegin()) seen++;
			}
			while(i<n && !v.get(i).value.isBegin()) sr.add(v.get(i++));
			r.add(Event.make(sr.get(0).time, sr));
		}
		return(r);
	}
	
	/*
	 * C<C<MidiMsg>>
	 * Using Stream of stream is possible but we have problems when
	 * taking values in the outer Streams when the inner is not closed...
	 */
	static C<C<MidiMsg>> countOnAna(Config config, C<MidiMsg> master, int count) {
		Peek<MidiMsg> pc = Peek.make(master);
		return(new C<C<MidiMsg>>() {
			public Event<C<MidiMsg>> get() throws EndOfStream {
				Event<MidiMsg> first = pc.peek();
				return(Event.make(first.time, new C<MidiMsg>() {
					public int seen = 0;
					//public Event<MidiMsg> ev = pc.peek();
					public Event<MidiMsg> get() throws EndOfStream {
						Event<MidiMsg> ev = pc.peek();
						if(ev.value.isBegin()) {
							if(seen == count) throw new EndOfStream("read enough On");
							seen++;
						}
						return(pc.get());
					}
				}));
			}});
	}
	
	static C<MidiMsg> vctrl(Config config) {
		C<MidiMsg> control1 = collector(config.inputs);
		C<MidiMsg> control;
		if(config.adjacentKeyFilter) {
			control = control1.filter(x -> forPierre.filter(x.value));
		} else {
			control = control1;
		}
		return(control);
	}

	
	
}
