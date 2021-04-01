package fr.inria.midifileperformer.app;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class DataTransfert implements ClipboardOwner {
	public static Clipboard clipboard;
	static {
		clipboard = new ClipboardWrapper
				(java.awt.Toolkit.getDefaultToolkit().getSystemClipboard());
	}

	public String get() {
		Transferable zap = clipboard.getContents(this);
		//System.out.println("get " + zap);
		try {
			if(zap.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				Object o = zap.getTransferData(DataFlavor.stringFlavor);
				//System.out.println(" STRING " + o);
				return((String) o);
			} else {
				System.out.println("NO WAY");
			}
		} catch(Exception err) {
			System.out.println("ERR " + err);
		}
		return("");
	}

	static class ClipboardWrapper extends Clipboard {
		Clipboard cb;
		ClipboardWrapper(Clipboard cb) {
			super(cb.getName());
			this.cb = cb;
		}
		public synchronized Transferable getContents(Object requestor) {
			Transferable contents = cb.getContents(requestor);
			if (testX11Selection(contents)) {
				//if (contents instanceof sun.awt.motif.X11Selection) {
				Transferable stringContents = null;
				try {
					stringContents = new StringSelection
							((String) contents.getTransferData(DataFlavor.stringFlavor));
				} catch (IOException e1) {
				} catch (UnsupportedFlavorException e1) {
				}
				return stringContents;
			} else {
				return contents;
			}
		}
		public synchronized void setContents(Transferable contents, 
				ClipboardOwner owner) {
			cb.setContents(contents, owner);
		}

	}

	private static final boolean testX11Selection(Object o) {
		try {
			Class<?> c = Class.forName("sun.awt.motif.X11Selection");
			return (o.getClass() == c);
		} catch (java.lang.Exception e) {
			return false;
		}
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

}
