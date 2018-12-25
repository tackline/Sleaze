package tackline.orphan;

public final class Sleaze {
   private Sleaze() {
   }
   /**
    * Do your worst.
    */
   @FunctionalInterface
   public interface Sleazy<R, EXC extends Throwable> {
      void sleaze(Sleazer<R, EXC> sleazer);
   }
   /**
    * Choose only one.
    */
   public interface Sleazer<R, EXC extends Throwable> {
      /**
       * Can't make up your mind what to get.
       */
      void of(SleazeSupplier<R, EXC> fn);
      /**
       * Nice path - pass a present.
       */
      void return_(R value);
      /**
       * Naughty path - throw coal.
       */
      void throw_(EXC exc);
   }
   /**
    * Like a pre-2018 Santa.
    */
   @FunctionalInterface
   public interface SleazeSupplier<R, EXC extends Throwable> {
      R get() throws EXC;
   }
    
   private enum State {
      UNSET, RETURN, THROW
   }  
   /**
    * Allows returning values and throws (one type of) exception
    *    through a chimney stack
    *    given a void returning, non-throwing interface.
    *    
    * <p>Suppose we want to write:
    * <pre>
    *   Integer result = things.unbox(thing -> {
    *      if (x) {
    *         return func(); // throws IOException
    *      } else if (y) {
    *         return 42;
    *      } else {
    *         throw new IOException();
    *      }
    *   }); });
    * </pre>
    * 
    * <p>But perhaps {@code unbox()} has a {@code void} return type and no throws clause.
    * Oh no.
    * But with Sleaze we can write something like:
    * <pre>
    *   Integer result = sleaze(IOException.class, sleaze -> { things.unbox(thing -> {
    *      if (x) {
    *         sleaze.of(() -> func());
    *      } else if (y) {
    *         sleaze.return_(42);
    *      } else {
    *         sleaze.throw_(new IOException());
    *      }
    *   }); });
    * </pre>
    * 
    * <p>I guess you could also write:
    * <pre>
    *   Integer result = sleaze(IOException.class, sleaze -> { things.unbox(thing -> { sleaze.of(() -> {
    *      if (x) {
    *         return func(); // throws IOException
    *      } else if (y) {
    *         return 42;
    *      } else {
    *         throw new IOException();
    *      }
    *   }); }); });
    * </pre>
    * If you wanted.
    */
   public static <R, EXC extends Throwable> R sleaze(Class<EXC> excClass, Sleazy<R, EXC> sleazy) throws EXC {
      class SleazerImpl implements Sleazer<R, EXC> {
         State state = State.UNSET;
         R value;
         EXC exc;
         public void of(SleazeSupplier<R, EXC> fn) {
            try {
                return_(fn.get());
            } catch (Throwable exc) {
               if (exc.getClass().isAssignableFrom(excClass)) { // !! CHECK !!
                  throw_(excClass.cast(exc));
               } else if (exc instanceof Error) {
                  throw (Error)exc;
               } else if (exc instanceof RuntimeException) {
                  throw (RuntimeException)exc;
               } else {
                  throw new IllegalArgumentException("Undeclared exception thrown", exc);
               } 
            }
         }
         public void return_(R value) {
            if (state != State.UNSET) {
               throw new IllegalStateException();
            }
            this.value = value;
            this.state = State.RETURN;
         }
         public void throw_(EXC exc) {
            if (state != State.UNSET) {
               throw new IllegalStateException();
            }
            this.exc = exc;
            this.state = State.THROW;
         }
      }
      SleazerImpl sleazer = new SleazerImpl();
      sleazy.sleaze(sleazer);
      switch (sleazer.state) {
         case RETURN: return sleazer.value;
         case THROW: throw sleazer.exc;
         case UNSET: default: throw new IllegalStateException("Expected a return or a throw");
      }
   }
}
