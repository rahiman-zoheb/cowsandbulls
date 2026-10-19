import React, { Component } from 'react';
import './App.css';
import Home from './Home';
import { BrowserRouter as Router, Redirect, Route, Switch } from 'react-router-dom';
import GuessList from './GuessList';
import NewGame from './NewGame';
import NotFound from './NotFound';

// Exported so tests can mount the real route table inside a MemoryRouter.
export function AppRoutes() {
  return (
    <Switch>
      <Route path='/' exact={true} component={Home}/>
      {/* Must precede /games/:gameId, which would otherwise match "new". */}
      <Route path='/games/new' exact={true} component={NewGame}/>
      <Route path='/games/:gameId' exact={true} component={GuessList}/>
      {/* Old links and bookmarks pointed here. */}
      <Redirect exact from='/guesses' to='/games/new'/>
      {/* Catch-all: an unmatched path used to render nothing at all. */}
      <Route component={NotFound}/>
    </Switch>
  );
}

class App extends Component {
  render() {
    return (
      <Router>
        <AppRoutes/>
      </Router>
    )
  }
}

export default App;
