import React, { Component } from 'react';
import { Alert, Button, Container, Table, Form, FormFeedback, FormGroup, Input, Label } from 'reactstrap';
import AppNavbar from './AppNavbar';
import { createGame, fetchGuesses, isValidGuess, normalizeGuess, submitGuess } from './api';

const EMPTY_ITEM = { name: '' };

const INVALID_GUESS_MESSAGE = 'Please provide a valid 4 letter word';

class GuessList extends Component {

  constructor(props) {
    super(props);
    this.state = {
      guesses: [],
      item: { ...EMPTY_ITEM },
      isLoading: true,
      isSubmitting: false,
      // Blank until the field is touched, so an untouched input shows neither
      // the valid nor the invalid style.
      touched: false,
      error: null
    };

    this.mounted = false;
    // Guards against an older GET landing after a newer one and reinstating a
    // stale list.
    this.latestRequest = 0;

    this.loadGuesses = this.loadGuesses.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  async componentDidMount() {
    this.mounted = true;
    try {
      // POST /api/game clears the list server-side, so it has to finish before
      // the list is read or the two race and the initial table is arbitrary.
      await createGame();
      await this.loadGuesses();
    } catch (error) {
      this.fail('Could not start a new game. Is the backend running?', error);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
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

  async loadGuesses() {
    const request = ++this.latestRequest;
    const guesses = await fetchGuesses();
    if (request === this.latestRequest) {
      this.setStateIfMounted({ guesses, isLoading: false, error: null });
    }
    return guesses;
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

    // The backend answers an invalid guess with an opaque 500, so it never gets
    // sent in the first place.
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
      await submitGuess(item.name);
      await this.loadGuesses();
      this.setStateIfMounted({ item: { ...EMPTY_ITEM }, touched: false, isSubmitting: false });
    } catch (error) {
      this.fail('Could not submit your guess. Please try again.', error);
    }
  }

  renderGuessRows() {
    return this.state.guesses.map((guess, index) => (
      <tr key={guess.id != null ? guess.id : `${index}-${guess.name}`}>
        <td style={{whiteSpace: 'nowrap'}}>{guess.name}</td>
        <td style={{whiteSpace: 'nowrap'}}>{guess.cows}</td>
        <td style={{whiteSpace: 'nowrap'}}>{guess.bulls}</td>
      </tr>
    ));
  }

  render() {
    const {guesses, item, isLoading, isSubmitting, touched, error} = this.state;

    const showValidity = touched && normalizeGuess(item.name).length > 0;
    const guessIsValid = isValidGuess(item.name);

    return (
      <div>
        {/* Rendered in every state, so a failed load still leaves a way out. */}
        <AppNavbar/>
        <Container>
          {error && <Alert color="danger" className="mt-3">{error}</Alert>}

          {isLoading ? (
            <p className="mt-3" role="status">Loading...</p>
          ) : (
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
              </FormGroup>
              <FormGroup className="mb-3">
                <Button color="primary" type="submit" disabled={isSubmitting || !guessIsValid}>
                  {isSubmitting ? 'Submitting...' : 'Submit'}
                </Button>
              </FormGroup>
            </Form>
          )}
        </Container>
        {!isLoading && <Container fluid>
          <h3>My guesses</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th style={{width: '50%'}}>Word</th>
              <th style={{width: '25%'}}>Cows (Right letter, Wrong place)</th>
              <th style={{width: '25%'}}>Bulls (Right letter, Right place)</th>
            </tr>
            </thead>
            <tbody>
            {guesses.length === 0 ? (
              <tr><td colSpan={3}>No guesses yet.</td></tr>
            ) : this.renderGuessRows()}
            </tbody>
          </Table>
        </Container>}
      </div>
    );
  }
}

export default GuessList;
