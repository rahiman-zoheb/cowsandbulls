import React, { Component } from 'react';
import { Alert, Button, Container } from 'reactstrap';
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
    this.setState({ error: null });
    try {
      const game = await createGame();
      if (this.mounted) {
        this.props.history.replace(`/games/${game.id}`);
      }
    } catch (error) {
      console.error('Could not start a new game', error);
      if (this.mounted) {
        this.setState({ error: 'Could not start a new game. Is the backend running?' });
      }
    }
  }

  render() {
    const { error } = this.state;
    return (
      <div>
        <AppNavbar/>
        <Container className="mt-3">
          {error ? (
            <>
              <Alert color="danger">{error}</Alert>
              <Button color="primary" onClick={this.start}>Try again</Button>
            </>
          ) : (
            <p role="status">Starting a new game...</p>
          )}
        </Container>
      </div>
    );
  }
}

export default NewGame;
