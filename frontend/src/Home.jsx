import { Component } from 'react';
import './App.css';
import AppNavbar from './AppNavbar';
import { Link } from 'react-router';

class Home extends Component {
  render() {
    return (
      <div>
        <AppNavbar/>
        <main className="container mt-4">
          {/* Every page needs exactly one <h1>; Home had no heading at all. */}
          <h1>Cows and Bulls</h1>
          <p className="lead">
            Guess the secret four-letter word. After each guess you&apos;ll see how many letters are
            {' '}<strong>bulls</strong> (right letter, right place) and how many are
            {' '}<strong>cows</strong> (right letter, wrong place).
          </p>
          {/* Rendered as the link itself: a <Link> nested inside a <button> is
              invalid HTML and puts two controls in the tab order for one action. */}
          <Link className="btn btn-primary mt-2" to="/games/new">Play A Game</Link>
        </main>
      </div>
    );
  }
}

export default Home;