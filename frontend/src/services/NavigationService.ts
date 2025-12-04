import { NavigationState } from '../types';

class NavigationService {
  private state: NavigationState = {
    currentScreen: 'menu',
    previousScreen: undefined,
  };

  getCurrentScreen(): string {
    return this.state.currentScreen;
  }

  getPreviousScreen(): string | undefined {
    return this.state.previousScreen;
  }

  navigateTo(screen: string): void {
    this.state.previousScreen = this.state.currentScreen;
    this.state.currentScreen = screen;
  }

  goBack(): string | undefined {
    if (this.state.previousScreen) {
      const target = this.state.previousScreen;
      this.state.previousScreen = undefined;
      this.state.currentScreen = target;
      return target;
    }
    return undefined;
  }

  reset(): void {
    this.state = {
      currentScreen: 'menu',
      previousScreen: undefined,
    };
  }
}

export const navigationService = new NavigationService();
export default NavigationService;
