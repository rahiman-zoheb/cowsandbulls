import React, { Component } from 'react';
import './App.css';
import Home from './Home';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import GuessList from './GuessList';
import NotFound from './NotFound';

class App extends Component {
  render() {
    return (
      <Router>
        <Switch>
          <Route path='/' exact={true} component={Home}/>
          <Route path='/guesses' exact={true} component={GuessList}/>
          {/* Catch-all: an unmatched path used to render nothing at all. */}
          <Route component={NotFound}/>
        </Switch>
      </Router>
    )
  }
}

export default App;
