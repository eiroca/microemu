/**
 * Copyright (C) 2006-2019 eIrOcA (eNrIcO Croce & sImOnA Burzio) - GPL >= 3.0
 * 
 * Portion Copyright (C) Juan Antonio Agudo
 *
 * Based upon Bubblet game written by Juan Antonio Agudo.
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/
 */
package net.eiroca.j2me.bubblet;

import net.eiroca.j2me.app.Application;
import net.eiroca.j2me.app.BaseApp;
import net.eiroca.j2me.game.GameApp;
import net.eiroca.j2me.game.GameScreen;

/**
 * The Class Bubblet.
 */
public class BubbletMIDlet extends GameApp {

  /**
   * Instantiates a new bubblet.
   */
  public BubbletMIDlet() {
    super();
    BaseApp.resPrefix = "bu";
    Application.background = 0x00000000;
    Application.foreground = 0x00FFFFFF;
    Application.menu = new short[][] {
        {
            GameApp.ME_MAINMENU, GameApp.MSG_MENU_MAIN_CONTINUE, GameApp.GA_CONTINUE, 0
        }, {
            GameApp.ME_MAINMENU, GameApp.MSG_MENU_MAIN_NEWGAME, GameApp.GA_NEWGAME, 1
        }, {
            GameApp.ME_MAINMENU, GameApp.MSG_MENU_MAIN_HIGHSCORE, GameApp.GA_HIGHSCORE, 2
        }, {
            GameApp.ME_MAINMENU, GameApp.MSG_MENU_MAIN_HELP, GameApp.GA_HELP, 5
        }, {
            GameApp.ME_MAINMENU, GameApp.MSG_MENU_MAIN_ABOUT, GameApp.GA_ABOUT, 6
        }
    };
    GameApp.hsName = "Bubblet";
  }

  /* (non-Javadoc)
   * @see net.eiroca.j2me.game.GameApp#getGameScreen()
   */
  public GameScreen getGameScreen() {
    return new BubbletScreen(this, 10, 10);
  }

  /* (non-Javadoc)
   * @see net.eiroca.j2me.game.GameApp#processGameAction(int)
   */
  public void processGameAction(final int action) {
    switch (action) {
      case GA_STARTUP: // Continue
        Application.show(null, gameMenu, true);
        break;
      case GA_CONTINUE: // Continue
        doGameResume();
        break;
      case GA_NEWGAME: // New game
        doGameStart();
        break;
      case GA_HIGHSCORE: // High score
        doHighScore();
        break;
      case GA_HELP:
        doHelp();
        break;
      case GA_ABOUT:
        doAbout();
        break;
      case GA_NEWHIGHSCORE:
        doSetNewHighScore();
        break;
      default:
        break;
    }
  }

}
