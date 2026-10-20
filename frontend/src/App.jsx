import { BrowserRouter, Navigate, Route, Routes, useNavigate, useParams } from 'react-router';
import './App.css';
import Home from './Home';
import GuessList from './GuessList';
import NewGame from './NewGame';
import NotFound from './NotFound';

// React Router 7 passes routing through hooks, which class components can't
// call. These wrappers read them and hand GuessList and NewGame plain props,
// so neither component needs rewriting.
function GameRoute() {
  const { gameId } = useParams();
  return <GuessList gameId={gameId}/>;
}

function NewGameRoute() {
  const navigate = useNavigate();
  // Replace, not push: Back then skips /games/new instead of creating another game.
  return <NewGame onCreated={id => navigate(`/games/${id}`, { replace: true })}/>;
}

// Exported so tests can mount the real route table inside a MemoryRouter.
// Static segments outrank parameters, so /games/new never matches :gameId.
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Home/>}/>
      <Route path="/games/new" element={<NewGameRoute/>}/>
      <Route path="/games/:gameId" element={<GameRoute/>}/>
      {/* Old links and bookmarks pointed here. */}
      <Route path="/guesses" element={<Navigate to="/games/new" replace/>}/>
      {/* Catch-all: an unmatched path used to render nothing at all. */}
      <Route path="*" element={<NotFound/>}/>
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes/>
    </BrowserRouter>
  );
}
