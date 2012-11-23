/* 

Soundplane Interface

Usage:

	p = Soundplane.new( touches, synthdef, recvPort );
	p = Soundplane.new;
	p.touch_set(5);  \\ (re)set number of touches
	p.touchNum; \\ query current number of touches
	p.synth(\default); \\ assign an already added SynthDef. 
	
	Note: synthdefs must have a \freq and a \gate in order to work properly, and they must not free themselves 
	
	ex: 	SynthDef(\SP_Default_Saw_Synth, {
				| out = 0, gate = 0, freq = 440, x, y, z|
				var snd, env;

				env = Env.adsr(0.01, 0, 1, 0.01);
				env = EnvGen.ar(env, gate);

				snd = Saw.ar(freq);
				snd = RLPF.ar(snd, freq * y * 8, 0.3 );
				snd = snd * env * z;
				snd = Pan2.ar(snd, x - 0.5);

				Out.ar(out, snd);

			})
	
		
First added: 10/07/2012
Updated: 11/23/2012

*/

Soundplane
{
	var <>touchNum, <synthdef, <recvPort;
	var <addr;
	var <voices, <touchResp, <touchAlive;
	
	*new 	{ | touchNum = 4, synthdef = \SP_Default_Saw_Synth, recvPort = 3123 |
		^super.newCopyArgs(touchNum, synthdef, recvPort ).init;
	}
	
	init	{
		
		// set up voice array
		voices = Array.fill(touchNum, nil);
		
		// add default synthdef and assign synth to voices, default synth is used if none provided
		fork {
			SynthDef(\SP_Default_Saw_Synth, {
				| out = 0, gate = 0, freq = 440, x, y, z|
				var snd, env;

				env = Env.adsr(0.01, 0, 1, 0.01);
				env = EnvGen.ar(env, gate);

				snd = Saw.ar(freq);
				snd = RLPF.ar(snd, freq * y * 8, 0.3 );
				snd = snd * env * z;
				snd = Pan2.ar(snd, x - 0.5);

				Out.ar(out, snd);

			}).add;
		
			Server.default.sync;
		
			this.synth(synthdef);
		};
				
		touchResp = OSCFunc( { | msg, time, addr, recvPort| 
			if(voices[(msg[1] - 1)].notNil) {voices[(msg[1] - 1)].setn( \freq, msg[5].midicps, \amp, msg[4], \x, msg[2], \y, msg[3], \z, msg[4])};
			}, '/t3d/tch', recvPort: recvPort, );
		
		touchAlive = OSCFunc( { | msg, time, addr, recvPort| 
			if(msg[1].notNil) {
					voices.do({|item, i| if(msg.includes((i + 1))) { item.setn(\gate, 1) } { item.setn(\gate, 0) }});
			}
			{voices.do({|item, i| if(item.notNil) { item.setn(\gate, 0) } })}; 
			}, '/t3d/alv', recvPort: recvPort, );
		

		
	}
	
	synth { | sdef = \SP_Default_Saw_Synth |
		synthdef = sdef;
		voices.do({|item, i| voices[i].free; voices[i] = Synth(synthdef)});
	}
	
	touch_set { | tNum |
		touchNum = tNum;
		voices.do({|item, i| if (voices[i] != nil) {voices[i].free};});		
		voices = Array.fill(touchNum, { Synth.new(synthdef) });		
	}
	
}