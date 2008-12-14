package org.apache.webbeans.sample.numberguess;


import java.lang.annotation.Annotation;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.webbeans.AnnotationLiteral;
import javax.webbeans.Current;
import javax.webbeans.Initializer;
import javax.webbeans.Named;
import javax.webbeans.SessionScoped;
import javax.webbeans.manager.Manager;

@Named
@SessionScoped
public class Game
{
   private int number;
   private boolean correct = false;
   private int guess = 1;
   private int smallest;
   private int biggest;
   private int remainingGuesses;   
   private @Current Manager manager;
   
   public Game()
   {
   }
   
   @Initializer
   Game(@Random int number, @MaxNumber int maxNumber)
   {
      this.number = number;
      this.smallest = 1;
      this.biggest = maxNumber;
      this.remainingGuesses = 10;
   }

   public int getNumber()
   {
      return number;
   }
   
   public int getGuess()
   {
      return guess;
   }
   
   public void setGuess(int guess)
   {
      this.guess = guess;
   }
   
   public int getSmallest()
   {
      return smallest;
   }
   
   public int getBiggest()
   {
      return biggest;
   }
   
   public int getRemainingGuesses()
   {
      return remainingGuesses;
   }
   
   public String clear()
   {
	   Annotation[] anns = new Annotation[1];
	   anns[0] = new AnnotationLiteral<Random>(){};

	   Annotation[] anns2 = new Annotation[1];
	   anns2[0] = new AnnotationLiteral<MaxNumber>(){};
	   
      this.number = manager.getInstanceByType(int.class, anns);
      this.smallest = 1;
      this.biggest = manager.getInstanceByType(int.class, anns2);
      this.remainingGuesses = 10;
      this.guess = 1;
      this.correct = false;
	    
	  return null;
   }
   
   
   public String check()
   {
 	  if(correct)
	  {
		  FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Game is over! Please restart the game..."));
		  return null;
	  }
	   
      if (guess>number)
      {
         biggest = guess - 1;
      }
      if (guess<number)
      {
         smallest = guess + 1;
      }
      if (guess == number)
      {
 		  correct = true;
	      FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Correct! Please restart the game..."));  
	      
	      return null;
     }
      
      if(remainingGuesses == 0)
      {
    	  FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Game is over! Please restart the game...")); 
    	  this.correct = false;
    	  
    	  return null;
      }
      else
      {
          remainingGuesses--;
      }
      
      return null;
   }
   
}
