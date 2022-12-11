import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickperov.stud.micro_notes_spring_boot.Application;
import com.nickperov.stud.micro_notes_spring_boot.api.Note;
import com.nickperov.stud.micro_notes_spring_boot.api.NoteDTO;
import com.nickperov.stud.micro_notes_spring_boot.service.NotesServiceImpl;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = Application.class)
@AutoConfigureMockMvc
public class NotesApplicationTest {

    private static final String BASE_URL = "/api/notes";
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private NotesServiceImpl notesService;

    @After
    public void cleanup() {
        notesService.cleanUp();
    }

    @Test
    public void testCreateNote() throws Exception {
        final var noteText = "Test note one";
        final var note = createNoteSuccess(noteText);
        assertEquals(noteText, note.getText());
        assertNotNull(note.getId());

        final var createdNote = getNoteSuccess(note.getId());
        assertEquals(noteText, createdNote.getText());
    }

    @Test
    public void testUpdateNote() throws Exception {
        final var originalText = "Test note one";
        final var note = createNoteSuccess(originalText);
        assertEquals(originalText, note.getText());
        assertNotNull(note.getId());
        final var updatedText = "Test note one modified";
        final var updNote = constructNote(note.getId(), updatedText);
        updateNoteSuccess(updNote);
        final var updatedNote = getNoteSuccess(note.getId());
        assertEquals(note.getId(), updatedNote.getId());
        assertEquals(updatedText, updatedNote.getText());
    }

    @Test
    public void testUpdateNoteBadRequest() throws Exception {
        final var originalText = "Test note 123456789";
        createNoteSuccess(originalText);

        final var updNote = constructNote(null, "New text");
        updateNoteBadRequest(updNote);
    }

    @Test
    public void testAddNoteBadRequest() throws Exception {
        final var note = constructNote(UUID.randomUUID(), "Some text 1234567");
        updateNoteBadUrl(note);
    }

    @Test
    public void testAddNote() throws Exception {
        final var text = "Test note add 123";
        final var id = UUID.randomUUID();
        final var note = constructNote(id, text);
        final var noteLocation = addNoteSuccess(note);
        final var newNote = getNoteSuccess(URI.create(noteLocation));
        assertEquals(note.getId(), newNote.getId());
        assertEquals(text, newNote.getText());
    }

    @Test
    public void testCreateMultipleNotes() throws Exception {
        final var noteOneText = "Test note one";
        final var noteTwoText = "Test note two";
        final var noteThreeText = "Test note three";
        final var noteFourText = "Test note four";
        // Test sort
        createNoteSuccess(noteOneText);
        Thread.sleep(10);
        createNoteSuccess(noteTwoText);
        Thread.sleep(10);
        createNoteSuccess(noteThreeText);
        Thread.sleep(10);
        createNoteSuccess(noteFourText);

        final var allNotes = getNotesSuccess();
        assertEquals(4, allNotes.size());
        assertEquals(noteFourText, allNotes.get(0).getText());
        assertEquals(noteThreeText, allNotes.get(1).getText());
        assertEquals(noteTwoText, allNotes.get(2).getText());
        assertEquals(noteOneText, allNotes.get(3).getText());
    }

    @Test
    public void testUpdateMultipleNotes() throws Exception {

        final var noteOneText = "Test note one";
        final var noteTwoText = "Test note two";
        final var noteThreeText = "Test note three";
        final var noteFourText = "Test note four";
        // Test sort
        createNoteSuccess(noteOneText);
        Thread.sleep(10);
        final var noteTwo = createNoteSuccess(noteTwoText);
        Thread.sleep(10);
        final var noteThree = createNoteSuccess(noteThreeText);
        Thread.sleep(10);
        createNoteSuccess(noteFourText);

        final var noteTwoUpdText = "Test note two update 222";
        final var noteThreeUpdText = "Test note three update 333";

        Thread.sleep(10);
        updateNoteSuccess(constructNote(noteTwo.getId(), noteTwoUpdText));
        Thread.sleep(10);
        updateNoteSuccess(constructNote(noteThree.getId(), noteThreeUpdText));

        final var allNotes = getNotesSuccess();
        assertEquals(4, allNotes.size());
        assertEquals(noteThreeUpdText, allNotes.get(0).getText());
        assertEquals(noteTwoUpdText, allNotes.get(1).getText());
        assertEquals(noteFourText, allNotes.get(2).getText());
        assertEquals(noteOneText, allNotes.get(3).getText());
    }

    @Test
    public void testDeleteNote() throws Exception {
        final var noteOne = "Note text 123456789";

        final var note = createNoteSuccess(noteOne);
        final var noteId = note.getId();
        final var createdNote = getNoteSuccess(noteId);
        assertNotNull(createdNote);
        deleteNoteSuccess(noteId);
        getNoteNotFound(noteId);
    }

    @Test
    public void testDeleteMultipleNotes() throws Exception {
        final var noteOne = "Note text 111";
        final var noteTwo = "Note text 222";
        final var noteThree = "Note text 333";
        final var noteFour = "Note text 444";
        final var noteFive = "Note text 555";

        final var allNotes = new String[]{noteOne, noteTwo, noteThree, noteFour, noteFive};
        final var allNoteIds = Arrays.stream(allNotes).map(this::createNoteSuccessNoException).map(Note::getId).toList();

        final var notesListInit = getNotesSuccess();
        assertNotNull(notesListInit);
        assertEquals(5, notesListInit.size());
        assertTrue(notesListInit.stream().map(NoteDTO::getText).allMatch(noteText -> new HashSet<>(List.of(allNotes)).contains(noteText)));

        deleteNoteSuccess(allNoteIds.get(3));
        deleteNoteSuccess(allNoteIds.get(4));

        final var notesListThreeElements = getNotesSuccess();
        assertNotNull(notesListThreeElements);
        assertEquals(3, notesListThreeElements.size());
        assertTrue(notesListThreeElements.stream().map(NoteDTO::getText).anyMatch(text -> text.equals(noteOne)));
        assertTrue(notesListThreeElements.stream().map(NoteDTO::getText).anyMatch(text -> text.equals(noteTwo)));
        assertTrue(notesListThreeElements.stream().map(NoteDTO::getText).anyMatch(text -> text.equals(noteThree)));

        deleteNoteSuccess(allNoteIds.get(0));
        deleteNoteSuccess(allNoteIds.get(1));
        deleteNoteSuccess(allNoteIds.get(2));

        final var notesListEmpty = getNotesSuccess();
        assertNotNull(notesListEmpty);
        assertTrue(notesListEmpty.isEmpty());
    }

    @Test
    public void testDeleteNoteNotFound() throws Exception {
        deleteNoteNotFound(UUID.randomUUID());
    }

    @Test
    public void testModelMapping() {
        final var id = UUID.randomUUID();
        final var text = "Test text";
        final var noteDTO = constructNote(id, text);
        assertEquals(id, noteDTO.getId());
        assertEquals(text, noteDTO.getText());
        assertNotNull(noteDTO.toString());
        assertTrue(noteDTO.toString().startsWith("NoteDTO"));
    }

    @Test
    public void testServiceModel() {
        final var text = "Test text";
        final var noteOne = notesService.createNote(text);
        assertNotNull(noteOne);
        assertEquals(text, noteOne.getText());
        assertNotNull(noteOne.toString());
        assertTrue(noteOne.toString().startsWith("NoteImpl"));

        final var noteTwo = notesService.createNote(text);
        assertNotEquals(noteOne, noteTwo);
        assertNotEquals(noteOne, null);
        assertEquals(noteOne, noteOne);

        final Set<Note> notesSet = new HashSet<>();
        notesSet.add(noteOne);
        notesSet.add(noteTwo);
        assertEquals(2, notesSet.size());
    }

    private NoteDTO constructNote(final UUID id, final String text) {
        return new NoteDTO(new Note() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getText() {
                return text;
            }
        });
    }

    private Note createNoteSuccess(final String noteText) throws Exception {
        final var result = mockMvc.perform(post(BASE_URL).contentType(MediaType.TEXT_PLAIN).content(noteText)).andExpect(status().isOk()).andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), NoteDTO.class);
    }

    private Note createNoteSuccessNoException(final String noteText) {
        try {
            return createNoteSuccess(noteText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updateNoteSuccess(final NoteDTO note) throws Exception {
        performUpdateNote(note).andExpect(status().isOk()).andReturn();
    }

    private void updateNoteBadRequest(final NoteDTO note) throws Exception {
        performUpdateNote(note).andExpect(status().isBadRequest()).andReturn();
    }

    private void updateNoteBadUrl(final NoteDTO note) throws Exception {
        performUpdateNote(note, BASE_URL + '/' + UUID.randomUUID()).andExpect(status().isBadRequest()).andReturn();
    }

    private ResultActions performUpdateNote(final NoteDTO note) throws Exception {
        return performUpdateNote(note, BASE_URL + '/' + note.getId());
    }

    private ResultActions performUpdateNote(final NoteDTO note, final String url) throws Exception {
        return mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(note)));
    }

    private NoteDTO getNoteSuccess(final UUID id) throws Exception {
        final var result = performGetNote(id)
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), NoteDTO.class);
    }

    private void getNoteNotFound(final UUID id) throws Exception {
        performGetNote(id).andExpect(status().isNotFound()).andReturn();
    }

    private ResultActions performGetNote(final UUID id) throws Exception {
        return mockMvc.perform(get(BASE_URL + '/' + id.toString()).contentType(MediaType.APPLICATION_JSON));
    }

    private NoteDTO getNoteSuccess(final URI uri) throws Exception {
        final var result = mockMvc.perform(get(uri).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), NoteDTO.class);
    }

    private List<NoteDTO> getNotesSuccess() throws Exception {
        final var result = mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
    }

    private String addNoteSuccess(final NoteDTO note) throws Exception {
        final var result = mockMvc.perform(put(BASE_URL + '/' + note.getId()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(note)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getHeader("Location");
    }

    private void deleteNoteSuccess(final UUID id) throws Exception {
        performNoteDelete(id).andExpect(status().isNoContent()).andReturn();
    }

    private void deleteNoteNotFound(final UUID id) throws Exception {
        performNoteDelete(id).andExpect(status().isNotFound()).andReturn();
    }

    private ResultActions performNoteDelete(final UUID id) throws Exception {
        return mockMvc.perform(delete(BASE_URL + '/' + id.toString()).contentType(MediaType.APPLICATION_JSON));
    }
}
