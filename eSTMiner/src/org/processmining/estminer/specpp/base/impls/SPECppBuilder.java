package org.processmining.estminer.specpp.base.impls;

import org.processmining.estminer.specpp.base.Candidate;
import org.processmining.estminer.specpp.base.PostProcessor;
import org.processmining.estminer.specpp.base.Result;
import org.processmining.estminer.specpp.componenting.data.DataRequirements;
import org.processmining.estminer.specpp.componenting.data.ParameterRequirements;
import org.processmining.estminer.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.estminer.specpp.componenting.evaluation.EvaluatorConfiguration;
import org.processmining.estminer.specpp.componenting.system.AbstractGlobalComponentSystemUser;
import org.processmining.estminer.specpp.componenting.system.GlobalComponentRepository;
import org.processmining.estminer.specpp.componenting.system.link.ComposerComponent;
import org.processmining.estminer.specpp.componenting.system.link.CompositionComponent;
import org.processmining.estminer.specpp.componenting.system.link.ProposerComponent;
import org.processmining.estminer.specpp.config.InitializingBuilder;
import org.processmining.estminer.specpp.config.PostProcessingConfiguration;
import org.processmining.estminer.specpp.config.ProposerComposerConfiguration;
import org.processmining.estminer.specpp.config.SupervisionConfiguration;
import org.processmining.estminer.specpp.config.parameters.SupervisionParameters;
import org.processmining.estminer.specpp.supervision.Supervisor;
import org.processmining.estminer.specpp.supervision.instrumentators.InstrumentedSPECpp;

import java.util.List;

public class SPECppBuilder<C extends Candidate, I extends CompositionComponent<C>, R extends Result, F extends Result> extends AbstractGlobalComponentSystemUser implements InitializingBuilder<SPECpp<C, I, R, F>, GlobalComponentRepository> {

    private final DelegatingDataSource<ProposerComposerConfiguration<C, I, R>> pcConfigDelegator = new DelegatingDataSource<>();
    private final DelegatingDataSource<PostProcessingConfiguration<R, F>> ppConfigDelegator = new DelegatingDataSource<>();
    private final DelegatingDataSource<SupervisionConfiguration> svConfigDelegator = new DelegatingDataSource<>();
    private final DelegatingDataSource<EvaluatorConfiguration> evConfigDelegator = new DelegatingDataSource<>();
    private final DelegatingDataSource<SupervisionParameters> svParametersDelegator = new DelegatingDataSource<>();


    public SPECppBuilder() {
        componentSystemAdapter().require(DataRequirements.proposerComposerConfiguration(), pcConfigDelegator)
                                .require(DataRequirements.postprocessingConfiguration(), ppConfigDelegator)
                                .require(DataRequirements.EVALUATOR_CONFIG, evConfigDelegator)
                                .require(DataRequirements.SUPERVISOR_CONFIG, svConfigDelegator)
                                .require(ParameterRequirements.SUPERVISION_PARAMETERS, svParametersDelegator);
    }

    @Override
    public SPECpp<C, I, R, F> build(GlobalComponentRepository gcr) {
        SupervisionConfiguration svConfig = svConfigDelegator.getData();
        List<Supervisor> supervisorList = svConfig.createSupervisors();
        for (Supervisor supervisor : supervisorList) {
            gcr.consumeEntirely(supervisor.componentSystemAdapter());
        }
        ProposerComposerConfiguration<C, I, R> pcConfig = pcConfigDelegator.getData();
        PostProcessingConfiguration<R, F> ppConfig = ppConfigDelegator.getData();
        EvaluatorConfiguration evConfig = evConfigDelegator.getData();
        evConfig.createEvaluators();
        ProposerComponent<C> proposer = pcConfig.createPossiblyInstrumentedProposer();
        ComposerComponent<C, I, R> composer = pcConfig.createPossiblyInstrumentedComposer();
        PostProcessor<R, F> processor = ppConfig.createPostProcessorPipeline();
        SupervisionParameters svParams = svParametersDelegator.getData();
        if (svParams.shouldBeInstrumented(SPECpp.class))
            return new InstrumentedSPECpp<>(gcr, supervisorList, proposer, composer, processor);
        else return new SPECpp<>(gcr, supervisorList, proposer, composer, processor);
    }
}