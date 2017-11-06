package com.jc.command;

import java.io.Serializable;
import java.util.EventObject;

/**
 * Base class that implements the command pattern by encapsulating a task to be performed.
 */
public abstract class Command implements Serializable {

   private static final long serialVersionUID = -429304070296372908L;

   private Command NextCommand;
   private EventObject EventObject;

   /**
    * Perform some command. Override this method to perform some specific function. Calls doNextCommand.
    * 
    * @see #doNextCommand
    */
   public void execute() throws Exception {
      if (NextCommand != null) {
         NextCommand.execute();
      }
   }

   /**
    * Sets an associated EventObject with the Command object.
    */
   public void setEventObject(EventObject eventObject) {
      EventObject = eventObject;
   }

   /**
    * Provides access to a Command object's associated EventObject.
    */
   public EventObject getEventObject() {
      return EventObject;
   }

   /**
    * Sets the next Command object to be executed after the current one is executed.
    */
   public void setNextCommand(Command nextCommand) {
      if (NextCommand == null) {
         NextCommand = nextCommand;
      }
      else {
         NextCommand.setNextCommand(nextCommand);
      }
   }

   /**
    * Returns the next command to be executed.
    */
   protected Command getNextCommand() {
      return NextCommand;
   }
}
