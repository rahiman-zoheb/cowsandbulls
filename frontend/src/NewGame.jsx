import { Component } from 'react';
import AppNavbar from './AppNavbar';
import { createGame } from './api';

// The only place a game is created. It replaces its own history entry with the
// game's URL, so Back skips it and a refresh reloads the same game instead of
// starting another one.
class NewGame extends Component {

  constructor(props) {
    super(props);
    this.state = { error: null };
    this.mounted = false;
    this.start = this.start.bind(this);
  }

  componentDidMount() {
    this.mounted = true;
    this.start();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  async start() {
    // In development, StrictMode unmounts and remounts this same instance,
    // and in a browser it does so after the first request has already been
    // sent. Without this guard the remount sent a second POST, so every page
    // load created two games. The request already in flight finishes the job:
    // it checks `mounted` when it resolves.
    if (this.creating) {
      return;
    }
    this.creating = true;
    this.setState({ error: null });
    try {
      const game = await createGame();
      if (this.mounted) {
        this.props.onCreated(game.id);
      }
    } catch (error) {
      console.error('Could not start a new game', error);
      if (this.mounted) {
        // Rate limited (429) or at capacity (503): the server says when to
        // retry, and "is the backend running?" would send the player looking
        // for an outage that isn't there.
        const busy = (error.status === 429 || error.status === 503) && error.detail;
        this.setState({ error: busy ? error.detail : 'Could not start a new game. Is the backend running?' });
      }
    } finally {
      this.creating = false;
    }
  }

  render() {
    const { error } = this.state;
    return (
      <div>
        <AppNavbar/>
        <main className="container mt-3">
          <h1 className="visually-hidden">New game</h1>
          {error ? (
            <>
              <div className="alert alert-danger" role="alert">{error}</div>
              <button type="button" className="btn btn-primary" onClick={this.start}>Try again</button>
            </>
          ) : (
            <p role="status">Starting a new game...</p>
          )}
        </main>
      </div>
    );
  }
}

export default NewGame;
