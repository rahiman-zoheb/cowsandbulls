import { Component } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Button, Container, Table, Form, FormFeedback, FormGroup, Input, Label } from 'reactstrap';
import AppNavbar from './AppNavbar';
import { fetchGame, isValidGuess, normalizeGuess, submitGuess } from './api';

const EMPTY_ITEM = { name: '' };

const INVALID_GUESS_MESSAGE = 'Please provide a valid 4 letter word';

// The server's reasons for these are meant for the player: not a word (422),
// already guessed or game over (409), malformed (400), slow down (429).
const REJECTED_GUESS_STATUSES = [400, 409, 422, 429];

// Plays the game named in the URL. Loading never creates a game, so a refresh
// resumes where you left off; only NewGame (/games/new) starts one.
class GuessList extends Component {

  constructor(props) {
    super(props);
    this.state = {
      game: null,
      item: { ...EMPTY_ITEM },
      isLoading: true,
      isSubmitting: false,
      notFound: false,
      // Blank until the field is touched, so an untouched input shows neither
      // the valid nor the invalid style.
      touched: false,
      error: null
    };

    this.mounted = false;

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentDidMount() {
    this.mounted = true;
    this.load();
  }

  componentDidUpdate(previous) {
    // Navigating straight from one game's URL to another keeps this component
    // mounted, so the new game has to be loaded here.
    if (previous.match.params.gameId !== this.gameId()) {
      this.load();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  gameId() {
    return this.props.match.params.gameId;
  }

  // setState is a no-op warning once the router has unmounted this component,
  // which happens whenever a request is still in flight on navigation.
  setStateIfMounted(update) {
    if (this.mounted) {
      this.setState(update);
    }
  }

  fail(message, error) {
    // Keeps the underlying cause visible without putting it in the UI.
    console.error(message, error);
    this.setStateIfMounted({ isLoading: false, isSubmitting: false, error: message });
  }

  async load() {
    const gameId = this.gameId();
    this.setState({ game: null, isLoading: true, notFound: false, error: null, item: { ...EMPTY_ITEM }, touched: false });
    try {
      const game = await fetchGame(gameId);
      // A slower response for a game we've since navigated away from must not
      // overwrite the current one.
      if (gameId === this.gameId()) {
        this.setStateIfMounted({ game, isLoading: false });
      }
    } catch (error) {
      if (error.status === 404) {
        this.setStateIfMounted({ isLoading: false, notFound: true });
      } else {
        this.fail('Could not load this game. Is the backend running?', error);
      }
    }
  }

  handleChange(event) {
    const { name, value } = event.target;
    this.setState(previous => ({
      item: { ...previous.item, [name]: value },
      touched: true
    }));
  }

  async handleSubmit(event) {
    event.preventDefault();

    const { item, isSubmitting } = this.state;

    // The server rejects malformed guesses; checking here avoids the round
    // trip and gives the player the reason straight away.
    if (!isValidGuess(item.name)) {
      this.setState({ touched: true, error: INVALID_GUESS_MESSAGE });
      return;
    }

    // Without this a double click posts the same guess twice.
    if (isSubmitting) {
      return;
    }

    this.setState({ isSubmitting: true, error: null });

    try {
      // The response is the whole updated game, so no second request is needed.
      const game = await submitGuess(this.gameId(), item.name);
      this.setStateIfMounted({ game, item: { ...EMPTY_ITEM }, touched: false, isSubmitting: false });
    } catch (error) {
      if (REJECTED_GUESS_STATUSES.includes(error.status) && error.detail) {
        // An expected rejection, not a failure: say why and keep the guess so
        // it can be corrected.
        this.setStateIfMounted({ isSubmitting: false, error: error.detail });
      } else {
        this.fail('Could not submit your guess. Please try again.', error);
      }
    }
  }

  renderNewGameButton(color = 'secondary') {
    return <Button color={color} tag={Link} to="/games/new">New game</Button>;
  }

  renderForm(game) {
    const { item, isSubmitting, touched } = this.state;
    const showValidity = touched && normalizeGuess(item.name).length > 0;
    const guessIsValid = isValidGuess(item.name);

    return (
      <Form onSubmit={this.handleSubmit}>
        <FormGroup className="mb-3">
          <Label for="name">Your guess (4 letters)</Label>
          <Input type="text" name="name" id="name" value={item.name}
                onChange={this.handleChange} autoComplete="off" maxLength={4}
                placeholder="Guess a 4 letter word"
                disabled={isSubmitting}
                valid={showValidity && guessIsValid}
                invalid={showValidity && !guessIsValid}
          />
          <FormFeedback>{INVALID_GUESS_MESSAGE}</FormFeedback>
          <p className="text-muted mt-1 mb-0">
            {game.attemptsLeft} {game.attemptsLeft === 1 ? 'guess' : 'guesses'} left
          </p>
        </FormGroup>
        <FormGroup className="mb-3">
          <Button color="primary" type="submit" className="me-2" disabled={isSubmitting || !guessIsValid}>
            {isSubmitting ? 'Submitting...' : 'Submit'}
          </Button>
          {this.renderNewGameButton()}
        </FormGroup>
      </Form>
    );
  }

  renderWin(game) {
    const attempts = game.guesses.length;
    return (
      <>
        <Alert color="success" className="mt-3">
          You got it in {attempts} {attempts === 1 ? 'guess' : 'guesses'}! The word was <strong>{game.answer}</strong>.
        </Alert>
        {this.renderNewGameButton('primary')}
      </>
    );
  }

  renderLoss(game) {
    return (
      <>
        <Alert color="secondary" className="mt-3">
          Out of guesses! The word was <strong>{game.answer}</strong>.
        </Alert>
        {this.renderNewGameButton('primary')}
      </>
    );
  }

  renderGame(game) {
    if (game.status === 'WON') {
      return this.renderWin(game);
    }
    if (game.status === 'LOST') {
      return this.renderLoss(game);
    }
    return this.renderForm(game);
  }

  renderGuesses(game) {
    return (
      <Container fluid className="mt-4">
        <h3>My guesses</h3>
        <Table className="mt-4">
          <thead>
          <tr>
            <th style={{width: '10%'}}>#</th>
            <th style={{width: '40%'}}>Word</th>
            <th style={{width: '25%'}}>Cows (Right letter, Wrong place)</th>
            <th style={{width: '25%'}}>Bulls (Right letter, Right place)</th>
          </tr>
          </thead>
          <tbody>
          {game.guesses.length === 0 ? (
            <tr><td colSpan={4}>No guesses yet.</td></tr>
          ) : game.guesses.map(guess => (
            <tr key={guess.attempt}>
              <td>{guess.attempt}</td>
              <td style={{whiteSpace: 'nowrap'}}>{guess.name}</td>
              <td style={{whiteSpace: 'nowrap'}}>{guess.cows}</td>
              <td style={{whiteSpace: 'nowrap'}}>{guess.bulls}</td>
            </tr>
          ))}
          </tbody>
        </Table>
      </Container>
    );
  }

  render() {
    const { game, isLoading, notFound, error } = this.state;

    return (
      <div>
        {/* Rendered in every state, so a failed load still leaves a way out. */}
        <AppNavbar/>
        <Container>
          {error && <Alert color="danger" className="mt-3">{error}</Alert>}

          {isLoading && <p className="mt-3" role="status">Loading...</p>}

          {notFound && (
            <>
              <Alert color="warning" className="mt-3">
                {"This game doesn't exist, or it has expired."}
              </Alert>
              {this.renderNewGameButton('primary')}
            </>
          )}

          {game && this.renderGame(game)}
        </Container>
        {game && this.renderGuesses(game)}
      </div>
    );
  }
}

export default GuessList;
