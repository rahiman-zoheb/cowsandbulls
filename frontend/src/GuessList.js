import React, { Component } from 'react';
import { Button, Container, Table, Form, FormFeedback, FormGroup, Input } from 'reactstrap';
import AppNavbar from './AppNavbar';

class GuessList extends Component {

  emptyItem = {
    name: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      guesses: [],
      validate: {},
      item: this.emptyItem,
      isLoading: true
    };

    this.createGame = this.createGame.bind(this);
    this.fetchGuesses = this.fetchGuesses.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  componentDidMount() {
    this.setState({isLoading: true});
    this.createGame();
    this.fetchGuesses();
  }

  createGame() {
    fetch('/api/game', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: '',
    });
  }

  fetchGuesses() {
    fetch('api/guesses')
      .then(response => response.json())
      .then(data => this.setState({guesses: data, isLoading: false}));
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let item = {...this.state.item};
    item[name] = value;
    this.setState({ item });
    this.validateGuess(event);
  }

  async handleSubmit(event) {
    event.preventDefault();
    const {item} = this.state;

    await fetch('/api/guess', {
      method: (item.id) ? 'PUT' : 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(item),
    });

    this.setState({
      item: this.emptyItem,
    });
    this.fetchGuesses();
  }

  validateGuess(event) {
    const { validate } = this.state;
    if (event.target.value.length === 4) {
      validate.guess = 'has-success'
    } else {
      validate.guess = 'has-danger'
    }

    this.setState({ validate });
  }

  render() {
    const {guesses, item, isLoading} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const guessList = guesses.map(guess => {
      return <tr key={guess.id}>
        <td style={{whiteSpace: 'nowrap'}}>{guess.name}</td>
        <td style={{whiteSpace: 'nowrap'}}>{guess.cows}</td>
        <td style={{whiteSpace: 'nowrap'}}>{guess.bulls}</td>
      </tr>
    });

    return (
      <div>
        <AppNavbar/>
        <Container>
          <Form onSubmit={this.handleSubmit}>
            <FormGroup>
              <Input type="text" name="name" id="name" value={item.name || ''}
                    onChange={this.handleChange} autoComplete="name" placeholder="Guess a 4 letter word"
                    valid={ this.state.validate.guess === 'has-success' }
                    invalid={ this.state.validate.guess === 'has-danger' }
              />
              <FormFeedback invalid>Please provide a valid 4 letter word</FormFeedback>
            </FormGroup>
            <FormGroup>
              <Button color="primary" type="submit">Submit</Button>
            </FormGroup>
          </Form>
        </Container>
        <Container fluid>
          <h3>My guesses</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="50%">Word</th>
              <th width="25%">Cows (Right letter, Wrong place)</th>
              <th width="25%">Bulls (Right letter, Right place)</th>
            </tr>
            </thead>
            <tbody>
            {guessList}
            </tbody>
          </Table>
        </Container>
      </div>
    );
  }
}

export default GuessList;