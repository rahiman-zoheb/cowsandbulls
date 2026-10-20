import { Component, createRef } from 'react';
import { Link } from 'react-router';
import AppNavbar from './AppNavbar';
import { fetchGame, isValidGuess, normalizeGuess, submitGuess } from './api';

const EMPTY_ITEM = { name: '' };

const INVALID_GUESS_MESSAGE = 'Please provide a valid 4 letter word';

// The server's reasons for these are meant for the player: not a word (422),
// already guessed or game over (409), malformed (400), slow down (429).
const REJECTED_GUESS_STATUSES = [400, 409, 422, 429];

const count = (n, singular, plural) => `${n} ${n === 1 ? singular : plural}`;

// What a screen reader hears after a guess. The table updates silently, so
// without this the score is only available by navigating to it.
export function describeGuess(game) {
  const latest = game.guesses[game.guesses.length - 1];
  return `${latest.name}: ${count(latest.cows, 'cow', 'cows')}, ${count(latest.bulls, 'bull', 'bulls')}. `
    + `${count(game.attemptsLeft, 'guess', 'guesses')} left.`;
}

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
      error: null,
      announcement: ''
    };

    this.mounted = false;
    this.inputRef = createRef();

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.focusInput = this.focusInput.bind(this);
  }

  componentDidMount() {
    this.mounted = true;
    this.load();
  }

  componentDidUpdate(previous) {
    // Navigating straight from one game's URL to another keeps this component
    // mounted, so the new game has to be loaded here.
    if (previous.gameId !== this.gameId()) {
      this.load();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  gameId() {
    return this.props.gameId;
  }

  // setState is a no-op warning once the router has unmounted this component,
  // which happens whenever a request is still in flight on navigation.
  setStateIfMounted(update, callback) {
    if (this.mounted) {
      this.setState(update, callback);
    }
  }

  focusInput() {
    if (this.inputRef.current) {
      this.inputRef.current.focus();
    }
  }

  fail(message, error) {
    // Keeps the underlying cause visible without putting it in the UI.
    console.error(message, error);
    this.setStateIfMounted({ isLoading: false, isSubmitting: false, error: message });
  }

  async load() {
    const gameId = this.gameId();
    this.setState({ game: null, isLoading: true, notFound: false, error: null, item: { ...EMPTY_ITEM }, touched: false, announcement: '' });
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
      const announcement = game.status === 'IN_PROGRESS' ? describeGuess(game) : '';
      // The input was disabled while submitting, which drops focus; put it back
      // so a keyboard player can type the next guess straight away.
      this.setStateIfMounted(
        { game, item: { ...EMPTY_ITEM }, touched: false, isSubmitting: false, announcement },
        this.focusInput
      );
    } catch (error) {
      if (REJECTED_GUESS_STATUSES.includes(error.status) && error.detail) {
        // An expected rejection, not a failure: say why and keep the guess so
        // it can be corrected.
        this.setStateIfMounted({ isSubmitting: false, error: error.detail }, this.focusInput);
      } else {
        this.fail('Could not submit your guess. Please try again.', error);
      }
    }
  }

  renderNewGameButton(color = 'secondary') {
    return <Link className={`btn btn-${color}`} to="/games/new">New game</Link>;
  }

  renderForm(game) {
    const { item, isSubmitting, touched } = this.state;
    const showValidity = touched && normalizeGuess(item.name).length > 0;
    const guessIsValid = isValidGuess(item.name);
    const validity = showValidity ? (guessIsValid ? ' is-valid' : ' is-invalid') : '';

    return (
      <form onSubmit={this.handleSubmit} className="mt-3">
        <div className="mb-3">
          <label htmlFor="name" className="form-label">Your guess (4 letters)</label>
          <input type="text" name="name" id="name" ref={this.inputRef}
                 className={`form-control${validity}`}
                 value={item.name} onChange={this.handleChange}
                 autoComplete="off" maxLength={4} placeholder="Guess a 4 letter word"
                 disabled={isSubmitting}
                 aria-invalid={showValidity && !guessIsValid}
                 aria-describedby="guess-feedback guesses-left"/>
          <div id="guess-feedback" className="invalid-feedback">{INVALID_GUESS_MESSAGE}</div>
          <p id="guesses-left" className="text-muted mt-1 mb-0">
            {count(game.attemptsLeft, 'guess', 'guesses')} left
          </p>
        </div>
        <div className="mb-3">
          <button type="submit" className="btn btn-primary me-2" disabled={isSubmitting || !guessIsValid}>
            {isSubmitting ? 'Submitting...' : 'Submit'}
          </button>
          {this.renderNewGameButton()}
        </div>
      </form>
    );
  }

  renderWin(game) {
    return (
      <>
        <div className="alert alert-success mt-3" role="status">
          You got it in {count(game.guesses.length, 'guess', 'guesses')}! The word was <strong>{game.answer}</strong>.
        </div>
        {this.renderNewGameButton('primary')}
      </>
    );
  }

  renderLoss(game) {
    return (
      <>
        <div className="alert alert-secondary mt-3" role="status">
          Out of guesses! The word was <strong>{game.answer}</strong>.
        </div>
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
      <section className="container-fluid mt-4" aria-labelledby="guesses-heading">
        <h2 id="guesses-heading" className="h3">My guesses</h2>
        {/* Scrolls sideways on narrow screens instead of overflowing the page. */}
        <div className="table-responsive">
          <table className="table mt-3" aria-labelledby="guesses-heading">
            <thead>
            <tr>
              <th scope="col" style={{width: '10%'}}>#</th>
              <th scope="col" style={{width: '40%'}}>Word</th>
              <th scope="col" style={{width: '25%'}}>Cows (Right letter, Wrong place)</th>
              <th scope="col" style={{width: '25%'}}>Bulls (Right letter, Right place)</th>
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
          </table>
        </div>
      </section>
    );
  }

  render() {
    const { game, isLoading, notFound, error, announcement } = this.state;

    return (
      <div>
        {/* Rendered in every state, so a failed load still leaves a way out. */}
        <AppNavbar/>
        <main>
          <div className="container">
            <h1 className="h2 mt-3">Guess the word</h1>
            {error && <div className="alert alert-danger mt-3" role="alert">{error}</div>}

            {isLoading && <p className="mt-3" role="status">Loading...</p>}

            {notFound && (
              <>
                <div className="alert alert-warning mt-3" role="status">
                  {"This game doesn't exist, or it has expired."}
                </div>
                {this.renderNewGameButton('primary')}
              </>
            )}

            {game && this.renderGame(game)}

            <p className="visually-hidden" aria-live="polite">{announcement}</p>
          </div>
          {game && this.renderGuesses(game)}
        </main>
      </div>
    );
  }
}

export default GuessList;
